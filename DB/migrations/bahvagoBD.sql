SET NAMES utf8mb4;
DROP DATABASE IF EXISTS bahvagoBD;

CREATE DATABASE bahvagoBD
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
USE bahvagoBD;

-- =========================================================
-- Localizacao: adicionados Estado e CEP (existiam em sql1.hoteis)
-- =========================================================
CREATE TABLE Localizacao (
    Latitude INT NOT NULL,
    Longitude INT NOT NULL,
    Cidade VARCHAR(50) NOT NULL,
    Estado VARCHAR(50) NOT NULL,
    Pais VARCHAR(50) NOT NULL,
    CEP VARCHAR(20) NOT NULL,
    EnderecoAproximado VARCHAR(60),
    PRIMARY KEY (Latitude , Longitude)
);

CREATE TABLE CriterioBusca (
    CodigoCriterioBusca int AUTO_INCREMENT,
    DataCheckin date NOT NULL,
    DataCheckOut date NOT NULL,
    NumHospedes int NOT NULL,
    Latitude int NOT NULL,
    Longitude int NOT NULL,
    PRIMARY KEY (CodigoCriterioBusca),
    FOREIGN KEY (Latitude,Longitude) REFERENCES Localizacao(Latitude,Longitude) ON DELETE CASCADE
);

-- =========================================================
-- Usuario: adicionados Email (login, como em sql1.usuarios) e DataCriacao
-- =========================================================
CREATE TABLE Usuario (
    CPF char(11),
    Email varchar(255) NOT NULL,
    Senha VARCHAR(255) NOT NULL,
    Nome varchar(50) NOT NULL,
    Tipo bit NOT NULL,
    DataCriacao datetime NOT NULL,
    CodigoCriterioBusca int NULL,
    PRIMARY KEY (CPF),
    UNIQUE KEY uk_usuario_email (Email),
    FOREIGN KEY (CodigoCriterioBusca) REFERENCES CriterioBusca(CodigoCriterioBusca) ON DELETE CASCADE
);

-- =========================================================
-- HotelEstatisticas: adicionados AvaliacaoMedia e DataCriacao (existiam em sql1.hoteis)
-- =========================================================
#NumeroAcesso
CREATE TABLE HotelEstatisticas (
    CodigoHotel int AUTO_INCREMENT,
    Nome varchar(100) NOT NULL,
    Descricao varchar(2000) NOT NULL,
    NumeroAcesso int,
    AvaliacaoMedia decimal(10,2),
    CPF char(11) NOT NULL,
    Latitude int NOT NULL,
    Longitude int NOT NULL,
    DataCriacao datetime NOT NULL,
    PRIMARY KEY (CodigoHotel),
    FOREIGN KEY (CPF) REFERENCES Usuario(CPF) ON DELETE CASCADE,
    FOREIGN KEY (Latitude,Longitude) REFERENCES Localizacao(Latitude,Longitude) ON DELETE CASCADE
);

-- =========================================================
-- Quarto: adicionados Tipo, Disponivel e DataCriacao (existiam em sql1.quartos)
-- =========================================================
CREATE TABLE Quarto (
    Numero int,
    Tipo varchar(100) NOT NULL,
    Preco double NOT NULL,
    Capacidade int NOT NULL,
    Descricao varchar(2000) NOT NULL,
    AceitaPet bit NOT NULL,
    Disponivel bit NOT NULL,
    DataCriacao datetime NOT NULL,
    CodigoHotel int NOT NULL,
    PRIMARY KEY (Numero, CodigoHotel),
    FOREIGN KEY (CodigoHotel) REFERENCES HotelEstatisticas(CodigoHotel) ON DELETE CASCADE
);

CREATE TABLE Oferta (
    CodigoOferta int AUTO_INCREMENT,
    UrlOrigem varchar(1000) NOT NULL,
    DataCheckIn date NOT NULL,
    DataCheckOut date NOT NULL,
    Preco double NOT NULL,
    Numero int NOT NULL,
    CodigoHotel INT NOT NULL,
    PRIMARY KEY (CodigoOferta),
    FOREIGN KEY (Numero,CodigoHotel) REFERENCES Quarto(Numero,CodigoHotel) ON DELETE CASCADE
);

CREATE TABLE Avaliacao (
    CodigoAvaliacao int AUTO_INCREMENT,
    Nota float NOT NULL,
    Comentario varchar(200),
    Data date NOT NULL,
    Resposta varchar(200),
    CodigoHotel int NOT NULL,
    CPF char(11) NOT NULL,
    PRIMARY KEY (CodigoAvaliacao),
    FOREIGN KEY (CodigoHotel) REFERENCES HotelEstatisticas(CodigoHotel) ON DELETE CASCADE,
    FOREIGN KEY (CPF) REFERENCES Usuario(CPF) ON DELETE CASCADE
);

CREATE TABLE Salva (
    CPF char(11) NOT NULL,
    CodigoOferta int NOT NULL,
    PRIMARY KEY (CPF, CodigoOferta),
    FOREIGN KEY (CPF) REFERENCES Usuario(CPF) ON DELETE CASCADE,
    FOREIGN KEY (CodigoOferta) REFERENCES Oferta(CodigoOferta) ON DELETE CASCADE
);

-- =========================================================
-- NOVO: Reserva -> equivalente a sql1.reservas.
-- Sem essa tabela, sql2 não representava uma reserva efetiva de quarto
-- (Oferta é apenas o anúncio externo, CriterioBusca é apenas parâmetro de busca).
-- =========================================================
CREATE TABLE Reserva (
    CodigoReserva int AUTO_INCREMENT,
    CPF char(11) NOT NULL,
    Numero int NOT NULL,
    CodigoHotel int NOT NULL,
    DataCheckin date NOT NULL,
    DataCheckOut date NOT NULL,
    Status varchar(50) NOT NULL,
    DataCriacao datetime NOT NULL,
    PRIMARY KEY (CodigoReserva),
    FOREIGN KEY (CPF) REFERENCES Usuario(CPF) ON DELETE CASCADE,
    FOREIGN KEY (Numero, CodigoHotel) REFERENCES Quarto(Numero, CodigoHotel) ON DELETE CASCADE
);

-- =========================================================
-- NOVO: HotelFavorito -> equivalente a sql1.favoritos.
-- Distinto de "Salva": Salva guarda uma OFERTA específica, HotelFavorito guarda o HOTEL inteiro.
-- =========================================================
CREATE TABLE HotelFavorito (
    CPF char(11) NOT NULL,
    CodigoHotel int NOT NULL,
    DataCriacao datetime NOT NULL,
    PRIMARY KEY (CPF, CodigoHotel),
    FOREIGN KEY (CPF) REFERENCES Usuario(CPF) ON DELETE CASCADE,
    FOREIGN KEY (CodigoHotel) REFERENCES HotelEstatisticas(CodigoHotel) ON DELETE CASCADE
);

-- =========================================================
-- INSERTS
-- =========================================================

INSERT INTO Localizacao (Latitude, Longitude, Cidade, Estado, Pais, CEP, EnderecoAproximado)
    VALUES (-23550520, -46633308, 'São Paulo', 'SP', 'Brasil', '01310-100', 'Av. Paulista, 1000'),
           (-23561414, -46655772, 'São Paulo', 'SP', 'Brasil', '01305-000', 'Rua Augusta, 500'),
           (-23548761, -46638590, 'São Paulo', 'SP', 'Brasil', '01419-000', 'Alameda Santos, 86');

INSERT INTO CriterioBusca
    (DataCheckin, DataCheckOut, NumHospedes, Latitude, Longitude)
    VALUES ('2026-06-15', '2026-06-19', 2, -23550520, -46633308),
           ('2026-07-16', '2026-07-20', 4, -23561414, -46655772),
           ('2026-08-17', '2026-08-21', 1, -23548761, -46638590);

-- 0 = cliente | 1 = gerente
INSERT INTO Usuario (CPF, Email, Senha, Nome, Tipo, DataCriacao, CodigoCriterioBusca)
    VALUES ('12345678900', 'roberto@bahvago.com', '$2a$10$tsA89aAbW3xBEb0UUcGyROLw/tFlKaiepUvPPWBe1kJRuY35bjxCa', 'Roberto Iakuti', 0, '2026-06-19 00:00:00', 1),
           ('98765432100', 'enzo@bahvago.com', '$2a$10$tsA89aAbW3xBEb0UUcGyROLw/tFlKaiepUvPPWBe1kJRuY35bjxCa', 'Enzo Saleiro', 1, '2026-06-19 00:00:00', 2),
           ('00000000000', 'mario@bahvago.com', '$2a$10$tsA89aAbW3xBEb0UUcGyROLw/tFlKaiepUvPPWBe1kJRuY35bjxCa', 'Mario Eduardo', 1, '2026-06-19 00:00:00', 3),
           ('00000000001', 'henrico@bahvago.com', '$2a$10$tsA89aAbW3xBEb0UUcGyROLw/tFlKaiepUvPPWBe1kJRuY35bjxCa', 'Henrico Valazo', 1, '2026-06-19 00:00:00', NULL),
           ('00000000002', 'patricio@bahvago.com', '$2a$10$tsA89aAbW3xBEb0UUcGyROLw/tFlKaiepUvPPWBe1kJRuY35bjxCa', 'Patricio Vargas', 1, '2026-06-19 00:00:00', NULL);

INSERT INTO HotelEstatisticas
    (Nome, Descricao, NumeroAcesso, AvaliacaoMedia, CPF, Latitude, Longitude, DataCriacao)
    VALUES('Mercure Sao Paulo Pinheiros','Paulista Avenue shops and restaurants are 4 km from this hotel. The hotel is located 10 km from São Paulo - Congonhas Airport and 33 km from São Paulo.',0,4.80,'98765432100',-23550520,-46633308,'2026-06-19 00:00:00'),
          ('Panamby Sao Paulo','The Hotel Panamby Sao Paulo is conveniently located just 600 meters from the Barra Funda subway and bus station. A complimentary breakfast and Wi-Fi are included, and valet service is available for a fee.',0,4.30,'00000000000',-23561414,-46655772,'2026-06-19 00:00:00'),
          ('Qoya São Paulo Paulista, Curio Collection by Hilton','O Qoya Hotel traz os conceitos de tranquilidade e bem-estar em conjunto com o melhor do design e da arquitetura para o equilíbrio completo da sua estada em meio ao principal ícone econômico e cultural de São Paulo.',0,5.00,'00000000001',-23548761,-46638590,'2026-06-19 00:00:00');

INSERT INTO Quarto (Numero, Tipo, Preco, Capacidade, Descricao, AceitaPet, Disponivel, DataCriacao, CodigoHotel)
    VALUES (101, 'simples', 350.00, 2, 'Quarto casal standard.', 0, 1, '2026-06-19 00:00:00', 1),
           (201, 'suite', 580.00, 2, 'SUPERIOR TWIN 2 PAX ', 1, 1, '2026-06-19 00:00:00', 2),
           (301, 'suite', 900.00, 2, 'Suíte executiva.', 1, 1, '2026-06-19 00:00:00', 3);

INSERT INTO Oferta
    (UrlOrigem, DataCheckIn, DataCheckOut, Preco, Numero,CodigoHotel)
    VALUES ('https://all.accor.com/booking/pt/accor/hotel/3147?dateIn=2026-07-15&nights=4&compositions=2&stayplus=false&snu=false&accessibleRooms=false&hideWDR=false&productCode=null&hideHotelDetails=false&utm_campaign=desktop-15072026-2-4-0&trv_reference=863a7db5-e2a3-31a2-94db-231e103eaeb7&utm_medium=partenariats&hmGUID=edcbf907-13be-43da-b71c-3081c67af78b&locale=BR&utm_source=Trivago&utm_content=BR-BR-BR-ALL&advertiser_id=247','2026-06-15','2026-06-19',620.00,101,1),
           ('https://maistrip.com/#/busca/hotel/1%7C254181/0/15-07-2026/19-07-2026/7/2/?trackingToken=e7d7bae9608b43488c38070e37cb03f008_06_2026_18_18_10&origin=trivago&trv_reference=b8121744-f68a-3a92-822d-93bd44a185e9','2026-07-16','2026-07-20',2900.00,201,2),
           ('https://ourtrip.com.br/pt_BR/hotel/110088?distribution=2&checkin=2026-07-15&checkout=2026-07-19&destination=Qoya+S%C3%A3o+Paulo+Paulista%2C+Curio+Collection+by+Hilton&code=110088&group=HOTEL&UTM_SOURCE=TRIVAGO&UTM_PARAMS=7cba4764194a64d135d1d72ac5625f114d9094482edec034cd6f91ce55472c14&currency=BRL&pp=ec10dac4-c41d-412c-bea9-7edbcb1cea45&trv_ref=c58e2dfd-af8d-31ef-9797-9bad490bcdb1&pos=BR','2026-08-17','2026-08-21',2545.72,301,3);

INSERT INTO Avaliacao
    (Nota, Comentario, Data, Resposta, CodigoHotel, CPF)
    VALUES(4.8,'Excelente localização e atendimento.','2026-06-19','Obrigado!',1,'12345678900'),
          (4.3,'Legalzinho.','2026-08-15','Esperamos recebê-lo novamente.',2,'12345678900'),
          (5.0,'Experiência Insana.','2026-09-10','Ficamos felizes com sua avaliação.',3,'12345678900');

INSERT INTO Salva
    VALUES ('12345678900', 1),
           ('12345678900', 2),
           ('12345678900', 3);

-- Reservas efetivas (equivalente a sql1.reservas)
INSERT INTO Reserva (CPF, Numero, CodigoHotel, DataCheckin, DataCheckOut, Status, DataCriacao)
    VALUES ('12345678900', 101, 1, '2026-06-15', '2026-06-19', 'ativa', '2026-06-19 00:00:00'),
           ('12345678900', 201, 2, '2026-07-16', '2026-07-20', 'concluida', '2026-06-19 00:00:00');

-- Hoteis favoritados (equivalente a sql1.favoritos)
INSERT INTO HotelFavorito (CPF, CodigoHotel, DataCriacao)
    VALUES ('12345678900', 1, '2026-06-19 00:00:00'),
           ('12345678900', 2, '2026-06-19 00:00:00'),
           ('12345678900', 3, '2026-06-19 00:00:00');


SELECT * FROM Localizacao;
SELECT * FROM CriterioBusca;
SELECT * FROM Usuario;
SELECT * FROM HotelEstatisticas;
SELECT * FROM Quarto;
SELECT * FROM Oferta;
SELECT * FROM Avaliacao;
SELECT * FROM Salva;
SELECT * FROM Reserva;
SELECT * FROM HotelFavorito;
