document.addEventListener("DOMContentLoaded", () => {

    const obterCookie = (nome) => {
        const cookieStr = `; ${document.cookie}`;
        const partes = cookieStr.split(`; ${nome}=`);
        if (partes.length === 2) {
            return partes.pop().split(';').shift();
        }
        return null;
    };

    const obterHeadersCsrf = () => {
        const csrfToken = obterCookie("XSRF-TOKEN");
        const headers = {};
        if (csrfToken) {
            headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfToken);
        }
        return headers;
    };

    const logoutServidor = async () => {
        const headers = {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            ...obterHeadersCsrf()
        };

        await fetch("/logout", {
            method: "POST",
            headers,
            credentials: "same-origin"
        });
    };

    // Verifica se existe a marcação de login salvo no localStorage do navegador
    const usuarioEstaLogado = localStorage.getItem('hotelhub_logged') === 'true';
    const adminEstaLogado = localStorage.getItem('hotelhub_admin_logged') === 'true';

    // ==========================================
    // 1. GERENCIAMENTO GLOBAL DA NAVBAR (SESSÃO)
    // ==========================================
    const navActions = document.querySelector(".nav-actions");
    const navActionsSearch = document.getElementById("navActionsSearch");
    const navActionsHotel = document.getElementById("navActionsHotel");
    const navActionsRoom = document.getElementById("navActionsRoom");

    // Agrupa todos os alvos possíveis de navbar das novas telas
    const targetNav = navActionsSearch || navActionsHotel || navActionsRoom || navActions;

    if (usuarioEstaLogado && targetNav) {
        targetNav.className = "nav-actions-logged";
        targetNav.innerHTML = `
            <a href="/favoritos" class="nav-icon-link" title="Favoritos"><i class="fa-regular fa-heart"></i></a>
            <a href="/usuarios/perfil" class="nav-icon-link" title="Minha Conta"><i class="fa-regular fa-user"></i></a>
            <button id="btnLogoutTop" class="btn-logout-icon" title="Sair"><i class="fa-solid fa-arrow-right-from-bracket"></i></button>
        `;
    }

    // Configura o botão de Logout global
    const interceptarLogout = document.getElementById("btnLogoutTop");
    if (interceptarLogout) {
        interceptarLogout.addEventListener("click", async (e) => {
            e.preventDefault();
            try {
                await logoutServidor();
            } catch (error) {
                console.error("Falha ao encerrar sessao no servidor", error);
            }
            localStorage.removeItem('hotelhub_logged');
            localStorage.removeItem('hotelhub_admin_logged');
            window.location.href = "/login?logout";
        });
    }

    // Form de busca da Home (Agora usa a action padrão HTML para o backend)
    const formularioBusca = document.getElementById("mainSearchForm");

    // Clique nos Cards de Hotel (Geral) -> Abre página do Hotel
    const cardsHoteis = document.querySelectorAll(".hotel-card");
    cardsHoteis.forEach((card) => {
        card.addEventListener("click", (e) => {
            gerenciarAcesso(e, "hotel.html");
        });
    });

    // ==========================================
    // 3. FLUXO INTERNO DA PÁGINA DO HOTEL
    // ==========================================
    const botoesVerOfertaQuarto = document.querySelectorAll(".btn-goToRoom");
    botoesVerOfertaQuarto.forEach(botao => {
        botao.addEventListener("click", (e) => {
            const numeroQuarto = botao.getAttribute("data-room");
            const codigoHotel = botao.getAttribute("data-hotel");
            
            if (numeroQuarto && codigoHotel) {
                window.location.href = `/quartos/hotel/${codigoHotel}/numero/${numeroQuarto}`;
            } else {
                if (typeof gerenciarAcesso === "function") {
                    gerenciarAcesso(e, `quarto.html?type=${numeroQuarto || 'deluxe'}`);
                } else {
                    window.location.href = `quarto.html?type=${numeroQuarto || 'deluxe'}`;
                }
            }
        });
    });

    // ==========================================
    // 4. FLUXO DO COMPARADOR DE SITES (quarto.html)
    // ==========================================
    const urlParams = new URLSearchParams(window.location.search);
    const quartoTipo = urlParams.get('type');

    const labelBread = document.getElementById("breadRoomName");
    const labelTitle = document.getElementById("roomMainTitle");

    if (quartoTipo && labelTitle) {
        if (quartoTipo === "standard") {
            if (labelBread) labelBread.textContent = "Quarto Standard";
            labelTitle.textContent = "Quarto Standard";
        } else if (quartoTipo === "master") {
            if (labelBread) labelBread.textContent = "Suite Master";
            labelTitle.textContent = "Suite Master";
        }
    }

    const botoesRedirecionamento = document.querySelectorAll(".btn-redirect-offer, #btnMainRedirect");
    botoesRedirecionamento.forEach(botao => {
        botao.addEventListener("click", () => {
            const urlDestino = botao.getAttribute("data-url") || "https://www.booking.com";
            window.open(urlDestino, '_blank');
        });
    });

    const thumbs = document.querySelectorAll(".room-thumbnails-strip .thumb");
    const mainImg = document.getElementById("roomDisplayImg");
    thumbs.forEach(thumb => {
        thumb.addEventListener("click", () => {
            thumbs.forEach(t => t.classList.remove("active"));
            thumb.classList.add("active");
            if (mainImg) mainImg.src = thumb.src;
        });
    });

    // ==========================================
    // 5. ABA DINÂMICA (PÁGINA DE PERFIL)
    // ==========================================
    const botoesAbas = document.querySelectorAll(".tab-btn");
    const conteudosAbas = document.querySelectorAll(".tab-content");

    if (botoesAbas.length > 0) {
        botoesAbas.forEach(botao => {
            botao.addEventListener("click", () => {
                const alvoAba = botao.getAttribute("data-tab");
                botoesAbas.forEach(b => b.classList.remove("active"));
                conteudosAbas.forEach(c => c.classList.remove("active"));
                botao.classList.add("active");
                const elementoAlvo = document.getElementById(`tab-${alvoAba}`);
                if (elementoAlvo) elementoAlvo.classList.add("active");
            });
        });
    }

    // ==========================================
    // 6. LÓGICA DE FAVORITOS (OFERTAS SALVAS)
    // ==========================================
    const favGrid = document.getElementById("favGrid");
    const botoesCoracao = document.querySelectorAll(".btn-heart[data-codigo-oferta]");
    const favoritosSalvos = new Set();

    const atualizarIconeCoracao = (botao, salvo) => {
        const icone = botao.querySelector("i");
        if (!icone) return;

        if (salvo) {
            icone.classList.remove("fa-regular");
            icone.classList.add("fa-solid");
            icone.style.color = "#ef4444";
            botao.classList.add("fav-active");
        } else {
            icone.classList.remove("fa-solid");
            icone.classList.add("fa-regular");
            icone.style.color = "white";
            botao.classList.remove("fav-active");
        }
    };

    const carregarFavoritos = async () => {
        try {
            const response = await fetch("/favoritos/ids", {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });

            if (response.status === 401) return;
            if (!response.ok) return;

            const ids = await response.json();
            ids.forEach((id) => favoritosSalvos.add(String(id)));

            document.querySelectorAll(".btn-heart[data-codigo-oferta]").forEach((botao) => {
                const codigoOferta = botao.getAttribute("data-codigo-oferta");
                if (favoritosSalvos.has(String(codigoOferta))) {
                    atualizarIconeCoracao(botao, true);
                }
            });
        } catch (error) {
            console.error("Falha ao carregar favoritos", error);
        }
    };

    const alternarFavorito = async (codigoOferta) => {
        const response = await fetch(`/favoritos/toggle/${codigoOferta}`, {
            method: "POST",
            headers: { "Accept": "application/json", ...obterHeadersCsrf() },
            credentials: "same-origin"
        });

        if (response.status === 401 || response.status === 302) {
            window.location.href = "/login";
            return null;
        }

        if (!response.ok) {
            const text = await response.text();
            throw new Error("Falha ao alternar favorito: " + response.status);
        }

        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
            return response.json();
        }

        throw new Error("Resposta inesperada do servidor (não é JSON)");
    };

    const removerFavorito = async (codigoOferta) => {
        const response = await fetch(`/favoritos/remover/${codigoOferta}`, {
            method: "DELETE",
            headers: { "Accept": "application/json", ...obterHeadersCsrf() },
            credentials: "same-origin"
        });

        if (response.status === 401) {
            window.location.href = "/login";
            return false;
        }

        return response.ok;
    };

    carregarFavoritos();

    botoesCoracao.forEach((botao) => {
        botao.addEventListener("click", async (e) => {
            e.stopPropagation();
            e.preventDefault();

            const codigoOferta = botao.getAttribute("data-codigo-oferta");
            if (!codigoOferta) return;

            if (favGrid) {
                const cardHotel = botao.closest(".hotel-card");
                const removido = await removerFavorito(codigoOferta);
                if (!removido) return;

                favoritosSalvos.delete(String(codigoOferta));
                if (cardHotel) cardHotel.remove();

                const itensRestantes = favGrid.querySelectorAll(".hotel-card").length;
                const contadorTexto = document.getElementById("favCount");
                if (contadorTexto) {
                    contadorTexto.textContent = `${itensRestantes} ${itensRestantes === 1 ? "oferta salva" : "ofertas salvas"}`;
                }
                if (itensRestantes === 0) {
                    const estadoVazio = document.getElementById("emptyState");
                    if (estadoVazio) estadoVazio.classList.remove("hidden");
                    if (favGrid) favGrid.style.display = "none";
                }
                return;
            }

            try {
                const resultado = await alternarFavorito(codigoOferta);
                if (!resultado) return;

                if (resultado.salvo) {
                    favoritosSalvos.add(String(codigoOferta));
                } else {
                    favoritosSalvos.delete(String(codigoOferta));
                }
                atualizarIconeCoracao(botao, resultado.salvo);
            } catch (error) {
                console.error("Erro ao salvar favorito", error);
            }
        });
    });

    // ===================================================
    // 7. AUTENTICAÇÃO DIRETA
    // ===================================================

    // CORREÇÃO: Só adiciona o evento se o link do gerente realmente existir
    const linkLoginGerente = document.getElementById("linkLoginGerente");
    if (linkLoginGerente) {
        linkLoginGerente.addEventListener("click", (e) => {
            e.preventDefault();
            window.location.href = "/login-hoteleiro";
        });
    }

    const autenticarUsuario = async (form, destinoSucesso, storageKey) => {
        const response = await fetch(form.action, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            },
            body: new URLSearchParams(new FormData(form)).toString(),
            credentials: "same-origin"
        });

        if (!response.ok || response.url.includes("error")) {
            throw new Error("Falha na autenticacao");
        }

        localStorage.setItem(storageKey, 'true');
        window.location.href = destinoSucesso;
    };

    // LOGIN DO HÓSPEDE
    const formLoginHospede = document.getElementById("formLoginHospede");
    if (formLoginHospede) {
        formLoginHospede.addEventListener("submit", async (e) => {
            e.preventDefault();
            try {
                await autenticarUsuario(formLoginHospede, "/", 'hotelhub_logged');
            } catch (error) {
                window.location.href = "/login?error";
            }
        });
    }

    // LOGIN DO GERENTE/HOTELEIRO
    const formLoginHoteleiro = document.getElementById("formLoginHoteleiro");
    if (formLoginHoteleiro) {
        formLoginHoteleiro.addEventListener("submit", async (e) => {
            e.preventDefault();
            try {
                await autenticarUsuario(formLoginHoteleiro, "/dashboard", 'hotelhub_admin_logged');
            } catch (error) {
                window.location.href = "/login-hoteleiro?error";
            }
        });
    }

    // Logout do Admin
    const btnAdminLogout = document.getElementById("btnAdminLogout");
    if (btnAdminLogout) {
        btnAdminLogout.addEventListener("click", async (e) => {
            e.preventDefault();
            try {
                await logoutServidor();
            } catch (error) {
                console.error("Falha ao encerrar sessao no servidor", error);
            }
            localStorage.removeItem('hotelhub_logged');
            localStorage.removeItem('hotelhub_admin_logged');
            window.location.href = "/login?logout";
        });
    }

    // Formulários Administrativos
    const formInfoHotel = document.getElementById("formInfoHotel");
    if (formInfoHotel) {
        formInfoHotel.addEventListener("submit", (e) => {
            e.preventDefault();
            alert("Alterações salvas com sucesso!");
        });
    }

    const formComodidadesHotel = document.getElementById("formComodidadesHotel");
    if (formComodidadesHotel) {
        formComodidadesHotel.addEventListener("submit", (e) => {
            e.preventDefault();
            alert("Comodidades atualizadas!");
        });
    }

    const formNovoQuarto = document.getElementById("formNovoQuarto");
    if (formNovoQuarto) {
        formNovoQuarto.addEventListener("submit", (e) => {
            e.preventDefault();
            alert("Quarto criado com sucesso!");
            window.location.href = "gerenciar-quartos.html";
        });
    }

    // Envio de respostas da Central de Avaliações
    const botoesResponderReview = document.querySelectorAll(".btn-submit-reply");
    botoesResponderReview.forEach(botao => {
        botao.addEventListener("click", () => {
            const containerForm = botao.closest(".admin-reply-form-zone");
            const caixaTexto = containerForm ? containerForm.querySelector("textarea") : null;

            if (caixaTexto && caixaTexto.value.trim() !== "") {
                caixaTexto.value = "";
                const cardReview = botao.closest(".admin-review-item-box");
                const badgeStatus = cardReview ? cardReview.querySelector(".status-pill") : null;
                if (badgeStatus) {
                    badgeStatus.textContent = "Respondido";
                    badgeStatus.style.backgroundColor = "rgba(34,197,94,0.15)";
                    badgeStatus.style.color = "#22c55e";
                }
                alert("Resposta enviada com sucesso!");
            }
        });
    });

    const botoesCancelarReview = document.querySelectorAll(".btn-cancel-reply");
    botoesCancelarReview.forEach(botao => {
        botao.addEventListener("click", () => {
            const containerForm = botao.closest(".admin-reply-form-zone");
            const caixaTexto = containerForm ? containerForm.querySelector("textarea") : null;
            if (caixaTexto) caixaTexto.value = "";
        });
    });

    // ==========================================
    // 8. GALERIA DE FOTOS DO HOTEL
    // ==========================================
    const galleryNavPrev = document.querySelector(".gallery-nav.prev");
    const galleryNavNext = document.querySelector(".gallery-nav.next");
    const galleryDots = document.querySelectorAll(".gallery-dots .dot");
    const mainGalleryImg = document.getElementById("mainGalleryImg");

    if (galleryNavPrev && galleryNavNext && mainGalleryImg) {
        const imagens = [];
        galleryDots.forEach(dot => {
            const src = dot.getAttribute("data-src");
            if (src && !imagens.includes(src)) {
                imagens.push(src);
            }
        });
        if (imagens.length === 0) {
            const initialSrc = mainGalleryImg.getAttribute("src") || mainGalleryImg.src;
            if (initialSrc) {
                imagens.push(initialSrc);
            }
        }

        let indiceAtual = 0;

        const atualizarGaleria = () => {
            if (imagens.length > 0) {
                mainGalleryImg.src = imagens[indiceAtual];
            }
            galleryDots.forEach((dot, index) => {
                if (index === indiceAtual) {
                    dot.classList.add("active");
                } else {
                    dot.classList.remove("active");
                }
            });
        };

        galleryNavPrev.addEventListener("click", () => {
            if (imagens.length > 0) {
                indiceAtual = (indiceAtual - 1 + imagens.length) % imagens.length;
                atualizarGaleria();
            }
        });

        galleryNavNext.addEventListener("click", () => {
            if (imagens.length > 0) {
                indiceAtual = (indiceAtual + 1) % imagens.length;
                atualizarGaleria();
            }
        });

        galleryDots.forEach((dot, index) => {
            dot.addEventListener("click", () => {
                if (index < imagens.length) {
                    indiceAtual = index;
                    atualizarGaleria();
                }
            });
        });
    }

    // ==========================================
    // 9. GRÁFICO DE HISTÓRICO DE PREÇOS
    // ==========================================
    const chartDots = document.querySelectorAll(".chart-dot");
    chartDots.forEach(dot => {
        dot.addEventListener("click", () => {
            chartDots.forEach(d => d.classList.remove("active"));
            dot.classList.add("active");
        });
    });

    // ==========================================
    // 10. FILTROS DE AVALIAÇÕES (ADMIN)
    // ==========================================
    const filterButtons = document.querySelectorAll(".admin-filter-btn");
    filterButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            filterButtons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
        });
    });

    // ==========================================
    // 11. CHECKBOX DE COMODIDADES
    // ==========================================
    const checkboxesComodidades = document.querySelectorAll(".admin-checkbox-list-stack input[type='checkbox']");
    checkboxesComodidades.forEach(checkbox => {
        checkbox.addEventListener("change", function () {
            // Aqui você pode adicionar lógica para salvar automaticamente
            console.log("Comodidade alterada:", this.nextElementSibling.textContent);
        });
    });

    // ==========================================
    // 12. UPLOAD DE FOTOS (SIMULAÇÃO)
    // ==========================================
    const uploadDropzone = document.querySelector(".upload-dropzone-box");
    if (uploadDropzone) {
        uploadDropzone.addEventListener("click", () => {
            alert("Funcionalidade de upload seria aberta aqui!");
        });

        uploadDropzone.addEventListener("dragover", (e) => {
            e.preventDefault();
            uploadDropzone.style.borderColor = "var(--accent-blue)";
        });

        uploadDropzone.addEventListener("dragleave", () => {
            uploadDropzone.style.borderColor = "rgba(255,255,255,0.12)";
        });

        uploadDropzone.addEventListener("drop", (e) => {
            e.preventDefault();
            uploadDropzone.style.borderColor = "rgba(255,255,255,0.12)";
            alert("Arquivos soltos! Upload seria iniciado.");
        });
    }

    // ==========================================
    // 5. FILTROS E ORDENAÇÃO DE RESULTADOS
    // ==========================================
    const filtersForm = document.getElementById("filtersForm");
    const sortSelect = document.getElementById("sortSelect");
    const hotelsGrid = document.getElementById("hotelsGrid");
    const priceRange = document.getElementById("priceRange");
    const priceRangeMax = document.getElementById("priceRangeMax");
    const clearFiltersBtn = document.getElementById("clearFilters");

    if (hotelsGrid && (filtersForm || sortSelect)) {
    const allCards = Array.from(hotelsGrid.querySelectorAll(".hotel-card"));

    const applyFiltersAndSort = () => {
        console.log("Aplicando filtros..."); 

        const maxPrice = priceRange ? parseFloat(priceRange.value) : Infinity;
        
        const petFriendlyCheckbox = document.querySelector('input[data-filter="pet"]');
        const petFriendlyChecked = petFriendlyCheckbox ? petFriendlyCheckbox.checked : false;

        const ratingCheckboxes = document.querySelectorAll('input[data-filter="rating"]:checked');
        const selectedRatings = Array.from(ratingCheckboxes).map(cb => parseFloat(cb.value));

        let visibleCards = allCards.filter(card => {
            const price = parseFloat(card.getAttribute("data-price"));
            const rating = parseFloat(card.getAttribute("data-rating"));
            const aceitaPet = card.getAttribute("data-pet") === 'true';

            if (price > maxPrice) return false;

            if (petFriendlyChecked && !aceitaPet) return false;

            if (selectedRatings.length > 0) {
                const minSelectedRating = Math.min(...selectedRatings);
                if (rating < minSelectedRating) return false;
            }

            return true;
        });

        if (sortSelect) {
            const sortValue = sortSelect.value;
            visibleCards.sort((a, b) => {
                const priceA = parseFloat(a.getAttribute("data-price"));
                const priceB = parseFloat(b.getAttribute("data-price"));
                const ratingA = parseFloat(a.getAttribute("data-rating"));
                const ratingB = parseFloat(b.getAttribute("data-rating"));

                if (sortValue === "preco_asc") return priceA - priceB;
                if (sortValue === "preco_desc") return priceB - priceA;
                if (sortValue === "avaliacao") return ratingB - ratingA;
                return 0;
            });
        }

        allCards.forEach(card => card.style.display = "none");
        
        visibleCards.forEach(card => {
            card.style.display = "block"; 
            hotelsGrid.appendChild(card);
        });

        const resultsCount = document.querySelector(".results-count");
        if (resultsCount) {
            resultsCount.textContent = `${visibleCards.length} ${visibleCards.length === 1 ? 'opção encontrada' : 'opções encontradas'}`;
        }

        const emptyState = document.querySelector(".empty-search-state");
        if (emptyState) {
            emptyState.style.display = visibleCards.length === 0 ? "flex" : "none";
        }
    };

    if (filtersForm) {
        filtersForm.addEventListener("submit", (e) => {
            console.log("Submit do formulário interceptado");
            e.preventDefault(); 
            applyFiltersAndSort();
            return false; 
        });

        if (priceRange) {
            priceRange.addEventListener("change", applyFiltersAndSort);
        }
    }

    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener("click", () => {
            console.log("Limpando filtros...");
            filtersForm.reset();
            if (priceRange) {
                priceRange.value = priceRange.max; 
            }
            if (priceRangeMax) {
                priceRangeMax.textContent = `R$ ${priceRange ? priceRange.max : '5000'}+`;
            }
            applyFiltersAndSort();
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", applyFiltersAndSort);
    }
}

    console.log("HotelHub - Sistema inicializado com sucesso! 🚀");
});