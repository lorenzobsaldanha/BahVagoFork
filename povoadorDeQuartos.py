import re
import unicodedata

import requests
import mysql.connector

API = "http://localhost:8001/api/v1/hoteis/ofertas"
CHECKIN = "2026-08-21"
CHECKOUT = "2026-08-23"

# Palavras/expressões que indicam "tag de oferta" e não fazem parte do nome do quarto
OFERTA_KEYWORDS = [
    "nao reembolsavel",
    "reembolsavel",
    "tarifa antecipada",
    "tarifa promocional",
    "pre-pagamento",
    "pre pagamento",
    "pagamento antecipado",
    "cancelamento gratuito",
    "sem reembolso",
    "oferta especial",
    "promocao",
    "desconto",
    "flash sale",
    "last minute",
    "melhor preco",
    "somente hoje",
    "oferta imperdivel",
]


def _sem_acentos(txt: str) -> str:
    txt = unicodedata.normalize("NFKD", txt)
    return "".join(c for c in txt if not unicodedata.combining(c))


def _normalizar(txt: str) -> str:
    return _sem_acentos(txt).lower().strip()


def _e_tag_oferta(segmento: str) -> bool:
    """Verifica se um pedaço do nome (separado por '-') é uma tag de oferta
    e não parte do nome real do quarto."""
    seg = segmento.strip()
    if not seg:
        return True

    seg_norm = _normalizar(seg)
    for kw in OFERTA_KEYWORDS:
        if kw in seg_norm:
            return True

    # Segmento curto e inteiramente em maiúsculas (ex: "NÃO REEMBOLSÁVEL")
    letras = re.sub(r"[^A-Za-zÀ-ÿ]", "", seg)
    if letras and letras.isupper() and len(seg.split()) <= 5:
        return True

    return False


def sanitizar_nome_quarto(nome: str) -> str:
    """Remove tags de oferta do nome do quarto, sejam elas separadas por
    hífen ou grudadas no início em caixa alta.

    Exemplos:
        "Apartamento Deluxe - Não Reembolsável" -> "Apartamento Deluxe"
        "TARIFA ANTECIPADA Quarto Standard com 2 camas individuais"
            -> "Quarto Standard com 2 camas individuais"
    """
    if not nome:
        return ""

    nome = nome.strip()

    # Remove prefixo em CAIXA ALTA colado ao nome (sem hífen)
    palavras = nome.split()
    i = 0
    while i < len(palavras):
        letras = re.sub(r"[^A-Za-zÀ-ÿ]", "", palavras[i])
        if letras and letras.isupper() and len(letras) > 1:
            i += 1
        else:
            break
    if 0 < i < len(palavras):
        nome = " ".join(palavras[i:])

    # Remove segmentos separados por hífen que sejam tags de oferta
    partes = re.split(r"\s*-\s*", nome)
    partes_validas = [p for p in partes if p.strip() and not _e_tag_oferta(p)]

    resultado = " - ".join(partes_validas).strip(" -")
    resultado = re.sub(r"\s+", " ", resultado).strip()

    return resultado if resultado else nome.strip()


def chave_dedup(nome: str) -> str:
    """Chave usada para detectar quartos já cadastrados (case/acento-insensitive)."""
    return _normalizar(nome)


def main():
    db = mysql.connector.connect(
        host="localhost",
        user="root",
        password="root",
        database="bahvagoBD",
    )
    cursor = db.cursor(dictionary=True)

    cursor.execute("SELECT CodigoHotel, Nome FROM HotelEstatisticas")
    hoteis = cursor.fetchall()

    numero_quarto = 1000

    for hotel in hoteis:
        print("Consultando", hotel["Nome"])

        # Acumula as ofertas de todas as buscas (1 a 5 pessoas) antes de gravar.
        # chave -> {"nome": str, "preco": float, "url": str, "capacidade": int}
        quartos_encontrados = {}

        try:
            for pessoas in range(1, 6):
                body = {
                    "hotel": hotel["Nome"],
                    "pessoas": pessoas,
                    "quartos": 1,
                    "dataCheckin": CHECKIN,
                    "dataCheckout": CHECKOUT,
                }

                r = requests.post(API, json=body, timeout=60)
                if r.status_code != 200:
                    print(f"  Erro ({pessoas} pessoa(s)):", r.status_code)
                    continue

                ofertas = r.json()["ofertas"]

                for oferta in ofertas:
                    nome_sanitizado = sanitizar_nome_quarto(oferta["quarto"])
                    if not nome_sanitizado:
                        continue

                    chave = chave_dedup(nome_sanitizado)

                    preco = float(
                        oferta["preco_total"]
                        .replace("R$", "")
                        .replace(".", "")
                        .replace(",", ".")
                        .strip()
                    )

                    # O quarto ainda aparece nesta busca com mais pessoas,
                    # então essa é (por enquanto) a maior capacidade confirmada.
                    # Sobrescreve os dados para refletir a config. dessa ocupação.
                    quartos_encontrados[chave] = {
                        "nome": nome_sanitizado,
                        "preco": preco,
                        "url": oferta["url"],
                        "capacidade": pessoas,
                    }

            # Só grava no banco depois de consolidar as 5 buscas do hotel
            for dados in quartos_encontrados.values():
                cursor.execute(
                    """
                    INSERT INTO Quarto
                        (Numero, Tipo, Preco, Capacidade, Descricao, AceitaPet, Disponivel, DataCriacao, CodigoHotel)
                    VALUES (%s,%s,%s,%s,%s,0,1,NOW(),%s)
                    """,
                    (
                        numero_quarto,
                        dados["nome"][:100],
                        dados["preco"],
                        dados["capacidade"],
                        dados["nome"],
                        hotel["CodigoHotel"],
                    ),
                )

                cursor.execute(
                    """
                    INSERT INTO Oferta
                        (UrlOrigem, DataCheckIn, DataCheckOut, Preco, Numero, CodigoHotel)
                    VALUES (%s,%s,%s,%s,%s,%s)
                    """,
                    (
                        dados["url"],
                        CHECKIN,
                        CHECKOUT,
                        dados["preco"],
                        numero_quarto,
                        hotel["CodigoHotel"],
                    ),
                )

                numero_quarto += 1

            db.commit()

        except Exception as e:
            print(e)

    cursor.close()
    db.close()


if __name__ == "__main__":
    main()
