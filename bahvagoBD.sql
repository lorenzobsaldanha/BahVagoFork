DROP DATABASE IF EXISTS bahvagoBD;

CREATE DATABASE bahvagoBD;

USE bahvagoBD;

CREATE TABLE Localizacao (
    Latitude INT NOT NULL,
    Longitude INT NOT NULL,
    Cidade VARCHAR(50) NOT NULL,
    Pais VARCHAR(50) NOT NULL,
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

CREATE TABLE Usuario (
    CPF char(11),
    Senha varchar(20) NOT NULL,
	Nome varchar(50) NOT NULL,
    Tipo bit NOT NULL,
    CodigoCriterioBusca int NULL,
    PRIMARY KEY (CPF),
    FOREIGN KEY (CodigoCriterioBusca) REFERENCES CriterioBusca(CodigoCriterioBusca) ON DELETE CASCADE
);

#NumeroAcesso 
CREATE TABLE HotelEstatisticas (
    CodigoHotel int AUTO_INCREMENT,
    Nome varchar(100) NOT NULL,
    Descricao varchar(2000) NOT NULL,
    NumeroAcesso int, 
    CPF char(11) NOT NULL,
    Latitude int NOT NULL,
    Longitude int NOT NULL,
    PRIMARY KEY (CodigoHotel),
    FOREIGN KEY (CPF) REFERENCES Usuario(CPF) ON DELETE CASCADE,
    FOREIGN KEY (Latitude,Longitude) REFERENCES Localizacao(Latitude,Longitude) ON DELETE CASCADE
);

CREATE TABLE Quarto (
    Numero int,
    Preco double NOT NULL,
    Capacidade int NOT NULL,
    Descricao varchar(2000) NOT NULL,
    AceitaPet bit NOT NULL,
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

INSERT INTO Localizacao 
	VALUES (-23550520, -46633308, 'São Paulo', 'Brasil', 'Av. Paulista, 1000'),
		   (-23561414, -46655772, 'São Paulo', 'Brasil', 'Rua Augusta, 500'),
		   (-23548761, -46638590, 'São Paulo', 'Brasil', 'Alameda Santos, 86');

INSERT INTO CriterioBusca
	(DataCheckin, DataCheckOut, NumHospedes, Latitude, Longitude)
	VALUES ('2026-06-15', '2026-06-19', 2, -23550520, -46633308),
		   ('2026-07-16', '2026-07-20', 4, -23561414, -46655772),
		   ('2026-08-17', '2026-08-21', 1, -23548761, -46638590);

-- 0 = cliente | 1 = gerente
INSERT INTO Usuario
	VALUES ('12345678900', 'abrobr1nh4', 'Roberto Iakuti', 0, 1),
		   ('98765432100', 'aaaa', 'Enzo Saleiro', 1, 2),
           ('00000000000', 'bbbb', 'Mario Eduardo', 1, 3),
           ('00000000001', 'cccc', 'Henrico Valazo', 1, NULL),
		   ('00000000002', 'dddd', 'Patricio Vargas', 1, NULL);
           

INSERT INTO HotelEstatisticas
	(Nome, Descricao, NumeroAcesso, CPF, Latitude, Longitude)
	VALUES('Mercure Sao Paulo Pinheiros','Paulista Avenue shops and restaurants are 4 km from this hotel. The hotel is located 10 km from São Paulo - Congonhas Airport and 33 km from São Paulo.',0,'98765432100',-23550520,-46633308),
		  ('Panamby Sao Paulo','The Hotel Panamby Sao Paulo is conveniently located just 600 meters from the Barra Funda subway and bus station. A complimentary breakfast and Wi-Fi are included, and valet service is available for a fee.',0,'00000000000',-23561414,-46655772),
          ('Qoya São Paulo Paulista, Curio Collection by Hilton','O Qoya Hotel traz os conceitos de tranquilidade e bem-estar em conjunto com o melhor do design e da arquitetura para o equilíbrio completo da sua estada em meio ao principal ícone econômico e cultural de São Paulo.',0,'00000000001',-23548761,-46638590);
          
INSERT INTO Quarto
	VALUES (101, 350.00, 2, 'Quarto casal standard.', 0, 1),
		   (201, 580.00, 2, 'SUPERIOR TWIN 2 PAX ', 1, 2),
		   (301, 900.00, 2, 'Suíte executiva.', 1, 3);

INSERT INTO Oferta
	(UrlOrigem, DataCheckIn, DataCheckOut, Preco, Numero,CodigoHotel)
	VALUES ('https://all.accor.com/booking/pt/accor/hotel/3147?dateIn=2026-07-15&nights=4&compositions=2&stayplus=false&snu=false&accessibleRooms=false&hideWDR=false&productCode=null&hideHotelDetails=false&utm_campaign=desktop-15072026-2-4-0&trv_reference=863a7db5-e2a3-31a2-94db-231e103eaeb7&utm_medium=partenariats&hmGUID=edcbf907-13be-43da-b71c-3081c67af78b&locale=BR&utm_source=Trivago&utm_content=BR-BR-BR-ALL&advertiser_id=247','2026-06-15','2026-06-19',620.00,101,1),
		   ('https://maistrip.com/#/busca/hotel/1%7C254181/0/15-07-2026/19-07-2026/7/2/?trackingToken=e7d7bae9608b43488c38070e37cb03f008_06_2026_18_18_10&origin=trivago&trv_reference=b8121744-f68a-3a92-822d-93bd44a185e9','2026-07-16','2026-07-20',2900.00,201,2),
           ('https://ourtrip.com.br/pt_BR/hotel/110088?distribution=2&checkin=2026-07-15&checkout=2026-07-19&destination=Qoya+S%C3%A3o+Paulo+Paulista%2C+Curio+Collection+by+Hilton&code=110088&group=HOTEL&UTM_SOURCE=TRIVAGO&UTM_PARAMS=7cba4764194a64d135d1d72ac5625f114d9094482edec034cd6f91ce55472c14&currency=BRL&pp=ec10dac4-c41d-412c-bea9-7edbcb1cea45&trv_ref=c58e2dfd-af8d-31ef-9797-9bad490bcdb1&pos=BR','2026-08-17','2026-08-21',2545.72,301,3);

INSERT INTO Avaliacao
	(Nota, Comentario, Data, Resposta, CodigoHotel, CPF)
	VALUES(4.8,'Excelente localização e atendimento.','2026-06-19','Obrigado!',1,'12345678900'),
		  (4.3,'Legalzinho.','2026-08-15','EspeAgenteramos recebê-lo novamente.',2,'12345678900'),
		  (5.0,'Experiência Insana.','2026-09-10','Ficamos felizes com sua avaliação.',3,'12345678900');

INSERT INTO Salva 
	VALUES ('12345678900', 1),
		   ('12345678900', 2),
		   ('12345678900', 3);


SELECT * FROM Localizacao;
SELECT * FROM CriterioBusca;
SELECT * FROM Usuario;
SELECT * FROM HotelEstatisticas;
SELECT * FROM Quarto;
SELECT * FROM Oferta;
SELECT * FROM Avaliacao;
SELECT * FROM Salva;