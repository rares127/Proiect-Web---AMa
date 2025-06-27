# AMa - Sistem de Management al Abrevierilor
## Specificația Cerințelor de Sistem

---

## Cuprins

1. [Introducere](#1-introducere)
2. [Descrierea generală](#2-descrierea-generală)
3. [Funcționalitățile sistemului](#3-funcționalitățile-sistemului)
4. [Cerințe non-funcționale](#4-cerințe-non-funcționale)
5. [Constrângeri tehnice](#5-constrângeri-tehnice)
6. [Videoclip prezentare](#6-videoclio-prezentare)

---

## 1. Introducere

### 1.1 Scopul documentului

Acest document specifică cerințele software pentru sistemul AMa (Abbreviation Management), o aplicație web destinată managementului, explorării, vizualizării și stocării abrevierilor cu semnificații multiple în diverse limbi. Sistemul permite utilizatorilor autentificați să creeze, să inventarieze și să partajeze informații despre abrevieri pe baza diferitelor caracteristici cum ar fi limba, domeniul și data.

### 1.2 Convenții de documentare

Documentul respectă template-ul IEEE System Requirements Specification. Cerințele sunt organizate în categorii funcționale și non-funcționale, fiecare cerință fiind identificată prin cod unic pentru ușurința referențierii.

### 1.3 Domeniul de aplicare

AMa este o aplicație web autonomă care gestionează datele despre abrevieri în format DocBook. Sistemul oferă interfețe web pentru interacțiunea cu utilizatorii și suportă exportul datelor în multiple formate, precum și generarea de statistici și flux RSS.

---

## 2. Descrierea generală

### 2.1 Perspectiva produsului

AMa funcționează ca o aplicație web independentă care stochează intern datele folosind formatul DocBook. Sistemul oferă interfețe web pentru interacțiunea utilizatorilor și funcționalități de export în multiple formate.

### 2.2 Funcțiile principale

Sistemul AMa oferă următoarele funcționalități principale:
- Sistem de autentificare și management al utilizatorilor cu roluri diferențiate
- Crearea, editarea și ștergerea abrevierilor, aprecierea, adaugarea la favorite
- Căutarea multi-criterială și filtrarea
- Generarea și exportul de statistici
- Generarea flux RSS pentru abrevierile populare
- Capacități de vizualizare detaliată și export de date

### 2.3 Tipurile de utilizatori

Sistemul suportă trei tipuri de utilizatori cu niveluri diferite de acces:

#### 2.3.1 Utilizatori vizitatori (Guest)
Utilizatorii neautentificați au acces limitat la funcționalitățile sistemului. Aceștia pot vizualiza dashboard-ul cu statistici generale și pot vedea abrevierile populare, dar nu pot crea, edita sau interacționa cu abrevierile prin aprecieri sau adăugare la favorite. Dar se pot folosi de functionalitati precum genereaza raport PDF, CSV sau flux RSS.

#### 2.3.2 Utilizatori înregistrați (User)
Utilizatorii autentificați beneficiază de acces complet la funcționalitățile standard ale sistemului. Aceștia pot crea și edita propriile abrevieri, pot aprecia și adăuga la favorite abrevierile altor utilizatori, pot accesa toate funcționalitățile de căutare și filtrare, și pot genera statistici pentru o abreviere selectata. In plus, acestia isi pot vedea propriile abrevieri, lista celor favorite cat si toate abrevierile din sistem.

#### 2.3.3 Administratori (Admin)
Administratorii au acces complet la toate funcționalitățile sistemului, inclusiv capacitatea de a edita și șterge orice abreviere din sistem, indiferent de autorul acesteia.

## 3. Funcționalitățile sistemului

### 3.1 Sistemul de autentificare

Sistemul de autentificare gestionează înregistrarea utilizatorilor, procesul de logare și menținerea sesiunilor de lucru.

#### 3.1.1 Înregistrarea utilizatorilor
Sistemul permite utilizatorilor să se înregistreze prin completarea unui formular cu username, email și parolă. Sistemul validează unicitatea username-ului și a adresei de email, verifică complexitatea parolei și creează automat un cont cu rolul "user". După înregistrarea cu succes, utilizatorul poate să se autentifice imediat în sistem.

#### 3.1.2 Autentificarea utilizatorilor
Procesul de autentificare permite utilizatorilor să se conecteze folosind username-ul sau email-ul împreună cu parola. Sistemul verifică credențialele și creează o sesiune de lucru validă. Parolele sunt stocate în format hash pentru securitate.

#### 3.1.3 Accesul ca vizitator
Sistemul permite accesul neautentificat cu funcționalități limitate. Vizitatorii pot accesa dashboard-ul și pot vizualiza statistici generale, dar nu pot crea sau interacționa cu abrevierile.

### 3.2 Modulul Dashboard

Dashboard-ul oferă o vedere de ansamblu asupra sistemului, prezentând statistici și abrevierile cele mai populare.

#### 3.2.1 Afișarea statisticilor generale
Dashboard-ul prezintă statistici incluzând numărul total de abrevieri din sistem, limbile disponibile, domeniile acoperite și contribuțiile utilizatorului curent (pentru utilizatorii autentificați). Aceste statistici sunt actualizate în timp real și oferă o imagine de ansamblu asupra activității platformei.

#### 3.2.2 Generarea și exportul rapoartelor statistice
Sistemul permite generarea de rapoarte detaliate în formatele CSV și PDF. Rapoartele includ distribuția pe limbi și domenii, top-ul abrevierilor după diverse criterii, și analiza activității recente.

#### 3.2.3 Generarea flux RSS
Sistemul generează automat un flux RSS pentru cele mai accesate abrevieri, actualizat periodic.

#### 3.2.4 Afișarea abrevierilor populare
Dashboard-ul prezintă primele 8 abrevieri cele mai populare, afișând pentru fiecare metadate complete: tag-uri cu limba și domeniul, data adăugării, username-ul creatorului, numărul de vizualizări, aprecieri și favorite.

#### 3.2.5 Vizualizarea detaliată din dashboard
Din dashboard, utilizatorii pot accesa vizualizarea completă a unei abrevieri, incluzând semnificația detaliată și descrierea completă. Această funcționalitate oferă acces rapid la informații fără a naviga către alte secțiuni.

#### 3.2.6 Exportul individual
Pentru fiecare abreviere afișată în dashboard, sistemul oferă opțiuni de export în formatele Markdown și HTML, permițând utilizatorilor să salveze și să partajeze informațiile într-un format convenabil.

### 3.3 Modulul de căutare

Modulul de căutare implementează funcționalități avansate de descoperire și filtrare a abrevierilor.

#### 3.3.1 Căutarea textuală
Sistemul oferă o bară de căutare care permite identificarea abrevierilor pe baza numelui, descrierii sau semnificațiilor. Căutarea este case-insensitive și suportă căutarea parțială.

#### 3.3.2 Filtrarea după limbă
Utilizatorii pot filtra rezultatele căutării după limba abrevierii, folosind un filtru dropdown care prezintă toate limbile disponibile în sistem. Filtrarea poate fi combinată cu alte criterii de căutare.

#### 3.3.3 Filtrarea după domeniu
Sistemul permite filtrarea abrevierilor după domeniul de aplicabilitate (tehnologie, medical, business, etc.), folosind un sistem de categorii predefinit. Acest filtru poate fi folosit independent sau în combinație cu alte criterii.

#### 3.3.4 Combinarea criteriilor de căutare
Sistemul suportă căutarea multi-criterială, permițând utilizatorilor să combine căutarea textuală cu filtrele de limbă și domeniu pentru rezultate mai precise și relevante.

#### 3.3.5 Afișarea rezultatelor cu opțiuni rapide
Rezultatele căutării sunt prezentate cu toate metadatele relevante și opțiuni rapide de acțiune (vizualizare, apreciere, adăugare la favorite) direct din lista de rezultate, fără a fi necesară navigarea către o pagină separată.

### 3.4 Modulul de management

Modulul de management oferă funcționalități complete pentru crearea, editarea și organizarea abrevierilor.

#### 3.4.1 Crearea abrevierilor noi
Utilizatorii autentificați pot crea abrevieri noi prin completarea unui formular cu textul abrevierii, semnificația completă, descrierea opțională, și clasificarea după limbă și domeniu. Sistemul validează datele introduse și previne duplicarea abrevierilor în aceeași combinație de limbă și domeniu.

#### 3.4.2 Managementul abrevierilor proprii
Sistemul oferă o interfață dedicată unde utilizatorii pot vizualiza și gestiona toate abrevierile create de ei. Această secțiune include opțiuni de editare, ștergere și vizualizare a statisticilor pentru fiecare abreviere.

#### 3.4.3 Colecția de favorite
Fiecare utilizator poate menține o colecție personală de abrevieri favorite, care poate fi accesată rapid din modulul de management.

#### 3.4.4 Vizualizarea tuturor abrevierilor
Sistemul oferă o interfață pentru explorarea tuturor abrevierilor din sistem.

#### 3.4.5 Operațiile de editare și ștergere
Pentru abrevierile pe care utilizatorii au permisiuni (propriile abrevieri pentru utilizatori normali, toate pentru administratori), sistemul oferă funcționalități complete de editare a tuturor câmpurilor și ștergere cu confirmare.

#### 3.4.6 Sistemul de interacțiuni
Utilizatorii pot interacționa cu abrevierile prin aprecieri (like) și adăugarea la favorite. Aceste interacțiuni sunt tracked și contribuie la calculul popularității abrevierilor. Fiecare interacțiune incrementează și contorul de vizualizări.

#### 3.4.7 Tracking-ul vizualizărilor
Sistemul înregistrează automat vizualizările abrevierilor atunci când utilizatorii interacționează cu ele prin aprecieri, adăugarea la favorite, sau vizualizarea detaliată, oferind date precise despre popularitatea conținutului.

---

## 4. Cerințe non-funcționale

### 4.1 Performanță

Dashboard-ul se încarcă în maximum 3 secunde în condiții normale de încărcare. Sistemul returnează rezultatele căutării în maximum 2 secunde pentru interogări pe baze de date cu până la 10.000 de abrevieri. Platforma suportă acces concurent de până la 100 de utilizatori simultan fără degradarea performanței.

### 4.2 Securitate

Parolele utilizatorilor sunt criptate folosind algoritmi de hash securizați. Sistemul implementează management al sesiunilor cu timeout automat și validează toate input-urile utilizatorilor pentru prevenirea atacurilor de tip injection.

---

## 5. Constrângeri tehnice

### 5.1 Constrângeri de dezvoltare

Conform cerințelor proiectului, sistemul este dezvoltat fără utilizarea de framework-uri externe. Dezvoltarea se bazează exclusiv pe tehnologii web standard (HTML, CSS, JavaScript) pentru frontend și Java cu servlets și JDBC pentru backend.

### 5.2 Arhitectura tehnică

Sistemul folosește o arhitectură în trei niveluri:
- **Frontend:** HTML, CSS, JavaScript vanilla (fără framework-uri)
- **Backend:** Java cu servlets
- **Baza de date:** PostgreSQL cu JDBC pentru persistența datelor

—

## 6. Videoclip prezentare

### 6.1 https://www.youtube.com/watch?v=-pKjEUIVVD0

---

*Sfârșit document*

