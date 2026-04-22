# ChatBloom — Specifica comportamentale definitiva per Codex

Implementa **ChatBloom** come plugin per la gestione completa della chat e delle feature collegate.

---

## Obiettivo del plugin

ChatBloom deve sostituire e centralizzare la gestione della chat del server in un unico sistema coerente.

Il risultato finale deve includere almeno queste aree:

1. chat pubblica formattata
2. sanitizzazione dell’input player
3. mentions normali
4. custom pings configurabili
5. toggle personale dei ping
6. messaggi privati
7. reply
8. PM spy
9. broadcast staff
10. cooldown anti-spam
11. word filter
12. chat items view-only
13. join / quit / first join
14. reload pulito
15. player data persistenti
16. console logging

Ogni feature deve essere implementata in modo che:

- sia prevedibile
- sia verificabile in gioco
- non rompa le altre feature
- abbia fallback sensati
- rispetti i permessi
- copra i casi limite principali

---

## Regole globali di comportamento

### Input del player sempre non affidabile

Qualsiasi testo scritto da un player in:

- chat pubblica
- messaggi privati
- broadcast inviato tramite funzionalità staff
- token contenuti dentro al messaggio

deve essere considerato **input non affidabile**.

Il plugin deve impedire che il player possa controllare il rendering finale scrivendo:

- MiniMessage
- codici legacy `&`
- HEX tipo `&#RRGGBB`
- click event
- hover event
- placeholder
- altri tag di formatting

#### Esempi obbligatori

Input:
`<red>ciao</red>`

Contenuto logico da trattare:
`ciao`

Input:
`&cciao`

Contenuto logico da trattare:
`ciao`

Input:
`<click:run_command:/op me>ciao</click>`

Contenuto logico da trattare:
`ciao`

### Regola pratica

Il plugin può usare Component, MiniMessage, PlaceholderAPI e rendering avanzato **solo per l’output deciso dal plugin**.

Il player fornisce **testo**.  
Il plugin fornisce **formato finale**.

---

### Il messaggio non va bloccato senza motivo

Se un messaggio contiene una feature opzionale che non può essere applicata, il messaggio deve comunque partire, salvo casi esplicitamente bloccanti.

#### Casi che NON devono bloccare il messaggio

- mention verso player offline
- custom ping usato senza permesso
- chatitem senza oggetto valido
- token sconosciuto
- custom ping con zero target online

#### Casi che POSSONO bloccare il messaggio

- cooldown attivo
- messaggio vuoto dopo sanitizzazione
- messaggio composto solo da spazi
- eventuale blocco deciso dal filtro, se previsto in modo esplicito

---

### Deduplicare gli effetti, non il testo

Se un target compare più volte nello stesso messaggio:

- il testo può essere evidenziato più volte
- la notifica deve partire una sola volta per target

Esempio:
`Steve Steve Steve guarda`

Risultato corretto:
- tutte le occorrenze possono essere highlightate
- Steve riceve una sola notifica totale

---

### Fallback sempre sensati

Se una parte avanzata della feature non è applicabile, il plugin non deve comportarsi in modo rotto o ambiguo.

Esempi:

- token chatitem non valido -> testo normale o fallback non cliccabile
- player senza permesso custom ping -> testo inviato senza effetto ping
- LuckPerms assente -> formato default
- PlaceholderAPI assente -> nessun crash, nessun blocco

---

## Ordine logico di processing dei messaggi pubblici

Quando un player invia un messaggio pubblico, il processing deve seguire questa logica concettuale:

1. verificare che il modulo chat sia attivo
2. controllare se il messaggio va bloccato dal cooldown
3. leggere e sanitizzare il contenuto player
4. scartare il messaggio se dopo sanitizzazione è vuoto o solo spazi
5. determinare mentions normali
6. determinare custom ping validi
7. costruire eventuali snapshot chatitem
8. applicare il filtro parole
9. comporre il messaggio finale formattato
10. inviare il messaggio in chat
11. inviare le notifiche ai target pingati
12. scrivere il log in console

### Regole importanti su questo ordine

- non notificare ping se il messaggio viene bloccato dal cooldown
- non interpretare MiniMessage player prima della sanitizzazione
- non creare preview chatitem live dopo che l’inventario del sender è cambiato
- il filtro parole deve lavorare sul testo sanitizzato
- la composizione finale deve avvenire quando i dati necessari sono già stati determinati

---

## Chat pubblica formattata

### Obiettivo

Sostituire completamente la chat vanilla con una chat gestita dal plugin.

### Comportamento richiesto

Quando un player scrive in chat:

1. il plugin intercetta l’evento
2. annulla il comportamento vanilla indesiderato
3. usa il contenuto sanitizzato
4. applica mentions, custom pings, filtro e chatitems
5. costruisce il messaggio finale come Component
6. invia il risultato finale ai player online

### Contenuto del formato

Il formato finale della chat deve poter includere almeno:

- eventuale prefisso plugin
- eventuale prefisso/rank del player
- nome del player
- messaggio processato

### LuckPerms

Se LuckPerms è presente, il plugin può usare:

- primary group
- prefisso gruppo
- eventuali formati differenziati per gruppo

Se LuckPerms non è presente:

- il plugin non deve rompersi
- deve usare un formato fallback stabile e leggibile

### Esempi pratici

Player: `Steve`  
Gruppo: `helper`  
Messaggio digitato: `ciao ragazzi`

Output concettuale:
`[Helper] Steve: ciao ragazzi`

Messaggio digitato:
`<red>ciao</red>`

Output concettuale:
`[Rank] Steve: ciao`

Non:
- rosso
- con tag visibili
- con MiniMessage interpretato dal player

### Casi limite obbligatori

- messaggio vuoto dopo sanitizzazione -> non inviare
- solo spazi -> non inviare
- input con tag malevoli -> inviare solo il testo pulito
- input con codici colore -> inviare solo il testo pulito
- placeholder del plugin non devono poter essere iniettati dal player

### Accettazione feature

La chat pubblica è corretta quando:

- la chat vanilla è sostituita
- il player non controlla il formatting finale
- il messaggio funziona insieme a mentions, filter e chatitems
- con e senza LuckPerms il comportamento resta coerente

---

## Mentions normali

### Obiettivo

Permettere di pingare un player scrivendo il suo nome in chat oppure usando un trigger esplicito come `@Nome`.

### Matching richiesto

Una mention normale deve attivarsi se il nome del player online appare come entità sensata nel messaggio.

#### Deve pingare

- `Steve guarda`
- `ciao Steve`
- `@Steve vieni qui`
- `Steve, sei online?`
- `Steve.`

#### Non deve pingare

- `Steve123` se il target è `Steve`
- `NotSteve`
- sottostringhe casuali dentro parole più lunghe
- nomi parziali non esatti

### Effetti della mention

Se una mention valida colpisce un player online:

1. il testo corrispondente deve essere evidenziato in chat
2. il target può ricevere:
   - suono
   - actionbar
   - altre notifiche locali previste
3. la notifica deve rispettare il toggle personale del target

### Highlight

La mention deve avere un rendering visivo chiaro e configurabile.

Esempi accettabili:

- `Steve` mostrato come `@Steve`
- `Steve` mantenuto ma colorato
- `@Steve` con prefisso e colore dedicati

### Self mention

Se il sender menziona se stesso:

- il testo può essere comunque evidenziato
- non deve ricevere notifica
- non deve generare rumore inutile

### Offline target

Se nel testo compare il nome di un player offline:

- il messaggio deve partire normalmente
- nessuno deve essere notificato
- nessun errore rumoroso

### Deduplicazione

Se il target è menzionato più volte nello stesso messaggio:

- si possono evidenziare tutte le occorrenze
- la notifica parte una sola volta

### Esempi pratici

Messaggio:
`Steve vieni al warp`

Risultato:
- `Steve` evidenziato
- Steve riceve suono
- Steve riceve actionbar

Messaggio:
`Steve Steve Steve`

Risultato:
- tutte le occorrenze possono essere colorate
- Steve riceve una sola notifica

Messaggio:
`NotSteve è diverso da Steve`

Risultato:
- solo `Steve` finale è mention valida

### Accettazione feature

La feature è corretta quando:

- il matching è sensato
- non fa match su sottostringhe sbagliate
- non spamma notifiche duplicate
- rispetta il toggle personale

---

## Custom pings configurabili

### Obiettivo

Permettere ping speciali completamente configurabili, per esempio:

- `@all`
- `@help`
- `@staff`
- `@builder`
- `@vip`

Ogni custom ping deve definire:

- chi può usarlo
- chi può essere bersaglio del ping
- come appare in chat

### Modello comportamentale obbligatorio

Il sistema deve essere **generalizzato**.  
`@all` e `@help` non devono essere hardcodati come eccezioni.

Ogni custom ping deve essere costruito a partire da:

- una chiave logica del ping
- un trigger visibile
- una regola per chi può usarlo
- una regola per chi può essere pingato
- un formato visivo del token in chat
- un boolean che indica se il ping deve bypassare il toggle dei singoli player

Interpretazione obbligatoria:

- la chiave logica genera il trigger visibile
- l’autorizzazione di uso decide chi può usare il ping
- l’autorizzazione di ricezione decide chi è bersaglio di quel ping
- il formato definisce il rendering del token in chat

### Matching del trigger

Il custom ping deve attivarsi solo sul token esatto.

#### Esempi corretti
Se esiste un custom ping con trigger `@all`:

- `@all` -> valido
- `@all,` -> valido con punteggiatura finale gestita correttamente
- `(@all)` -> valido se il parsing gestisce parentesi/punteggiatura in modo sensato

#### Esempi non validi

- `ciao@all`
- `@alll`
- `xx@allxx`
- sottostringhe accidentali

### Uso autorizzato e non autorizzato

#### Caso autorizzato
Se il sender è autorizzato:
- il custom ping viene processato
- i target vengono selezionati
- il token viene renderizzato come custom ping

#### Caso non autorizzato
Se il sender non è autorizzato:
- il messaggio deve comunque partire
- quel token non deve produrre effetti ping
- nessuno deve essere notificato
- il token può restare testo normale oppure non essere trattato come ping speciale

### Regola importante
Non bloccare l’intero messaggio solo perché il sender ha scritto un custom ping senza permesso.

### Selezione dei destinatari

Quando un custom ping valido è usato da un sender autorizzato:

- vengono selezionati tutti i player online autorizzati a riceverlo
- il toggle ping personale deve essere rispettato
- il sender non deve auto-notificarsi inutilmente

### Effetti del custom ping

Un custom ping valido deve:

1. avere un rendering speciale in chat
2. notificare i target selezionati
3. rispettare il toggle personale
4. evitare notifiche duplicate

### Zero target online

Se nessun player online rientra nei destinatari:

- il messaggio deve partire comunque
- il token può rimanere visivamente evidenziato
- nessuno viene notificato
- nessun errore rumoroso

### Interazione con mentions normali

Se lo stesso player è colpito nello stesso messaggio sia da:

- una mention nominale
- un custom ping

deve ricevere **una sola notifica totale**.

Il testo può invece mantenere entrambi gli highlight.

### Esempi pratici

Caso:
- solo lo staff può usare `@all`
- i player normali possono esserne destinatari

Messaggio:
`@all riavvio tra 2 minuti`

Risultato:
- `@all` evidenziato
- tutti i target online ricevono notifica
- chi ha toggle off non riceve notifica se non e' forzato nella definizione del ping

Messaggio:
`@all guardate lo spawn`

Caso:
- sender non autorizzato

Risultato:
- messaggio inviato
- nessun ping effettivo
- nessuna notifica globale

Caso:
- esiste un ping `@help`
- solo un certo ruolo può riceverlo

Messaggio:
`@help potete venire in spawn?`

Risultato:
- solo i target online coerenti ricevono notifica

### Accettazione feature

La feature è corretta quando:

- i custom ping non sono hardcodati
- ogni custom ping ha regole separate di uso e target
- l’uso non autorizzato non blocca il messaggio
- il toggle personale funziona anche qui
- i destinatari vengono deduplicati

---

## Toggle personale dei ping

### Obiettivo

Ogni player deve poter decidere se ricevere o meno notifiche di ping. (salvo ping forzati nella definizione)

### Ambito del toggle

Il toggle deve disattivare almeno:

- suono mention normale
- actionbar mention normale
- notifiche da custom ping
- eventuali notifiche analoghe gestite dal plugin

Il toggle **non** deve nascondere i messaggi in chat.

### Persistenza

Il valore deve restare salvato dopo:

- relog
- restart
- reload plugin

### Default

Default consigliato:
- ping attivi per i player nuovi

### Esempi pratici

- player disattiva ping -> continua a vedere `@all riavvio`, ma non sente suono e non riceve actionbar
- player riattiva ping -> torna a ricevere notifiche normalmente

### Accettazione feature

La feature è corretta quando:

- il toggle è persistente
- agisce solo sulle notifiche
- vale sia per mentions normali sia per custom pings

---

## Messaggi privati

### Obiettivo

Offrire un sistema di PM locale al server, coerente con il resto del plugin.

### Comportamento base

Quando un player invia un PM:

1. il target deve essere online
2. il contenuto va sanitizzato
3. il plugin applica il formato corretto
4. il sender vede il formato “verso”
5. il target vede il formato “da”
6. viene aggiornata la relazione per il reply

### Reply

Deve esistere una funzione di risposta all’ultimo interlocutore privato rilevante.

#### Regola pratica

- se A scrive a B -> A e B possono rispondersi a vicenda
- se poi C scrive a B -> il riferimento di B diventa C
- il comportamento deve essere coerente e prevedibile

### Sanitizzazione e moderazione

I PM devono seguire le stesse regole di sicurezza base della chat pubblica:

- sanitizzazione input
- eventuale word filter
- eventuale cooldown se previsto dal progetto finale

### Suono PM

Alla ricezione di un PM, il target può ricevere una notifica sonora dedicata, distinta dai ping normali.

### Suggerimenti player online

Quando possibile, la funzionalità di invio PM deve suggerire i player online, escludendo il sender stesso.

### Edge case obbligatori

- target offline -> errore chiaro
- target uguale al sender -> blocco o comportamento coerente non abusabile
- messaggio vuoto -> non inviare
- solo spazi -> non inviare

### Esempi pratici

Caso:
A invia a B il testo `ciao`

B vede:
`[PM] A -> te: ciao`

A vede:
`[PM] tu -> B: ciao`

Dopo il messaggio precedente, la funzione di reply deve indirizzare il testo a B.

### Accettazione feature

La feature è corretta quando:

- PM e reply funzionano in modo coerente
- il contesto ultimo interlocutore è aggiornato correttamente
- il formato sender/receiver è distinto
- i PM condividono le regole di sicurezza base della chat

---

## PM Spy

### Obiettivo

Permettere allo staff autorizzato di vedere una copia dei PM per moderazione.

### Comportamento

Quando lo spy è attivo:

- lo staffer riceve una copia dei PM tra altri player

Quando lo spy è disattivo:

- non riceve nulla

### Regole importanti

- solo chi è autorizzato può usare lo spy
- il formato spy deve essere chiaramente diverso da un PM normale
- lo spy non deve alterare il flusso dei PM originali

### Duplicati da evitare

Se lo staffer con spy attivo è coinvolto direttamente nel PM:

- non deve ricevere una copia spy inutile in aggiunta ai messaggi normali già suoi

### Persistenza

Lo stato spy può essere:
- persistente
oppure
- limitato alla sessione

Scelta consigliata:
- persistente, se i player data sono già gestiti in modo pulito

### Esempio

A scrive a B:
`ciao`

Lo staffer con spy attivo vede:
`[SPY] A -> B: ciao`

### Accettazione feature

La feature è corretta quando:

- è un vero toggle
- non crea duplicati inutili
- è chiaramente separata dai PM normali

---

## Broadcast staff

### Obiettivo

Consentire allo staff di inviare annunci visibili a tutto il server.

### Comportamento

Quando un player autorizzato usa la funzionalità broadcast:

1. il messaggio viene letto
2. viene sanitizzato
3. viene applicato il formato broadcast
4. viene inviato a tutti i player online

### Differenza dalla chat normale

Il broadcast deve risultare immediatamente diverso dalla chat player normale tramite:

- prefisso dedicato
- colore dedicato
- formato dedicato
- eventuale suono, se previsto

### Esempio

Messaggio broadcast:
`Server restart tra 5 minuti`

Risultato:
- messaggio staff globale chiaramente distinguibile dalla chat normale

### Accettazione feature

La feature è corretta quando:

- l’autorizzazione è rispettata
- il formato è chiaramente distinto
- il messaggio arriva a tutti i player online

---

## Cooldown anti-spam

### Obiettivo

Impedire flood e spam imponendo un intervallo minimo tra messaggi.

### Ambito minimo

Il cooldown deve applicarsi almeno alla chat pubblica.

Può applicarsi anche ai PM se il progetto finale lo decide, ma il comportamento deve essere esplicito e coerente.

### Comportamento richiesto

Se il player prova a inviare un messaggio troppo presto:

- il nuovo messaggio viene bloccato
- il player riceve un messaggio chiaro
- idealmente vede il tempo residuo o i secondi mancanti

### Bypass

Deve esistere una regola di bypass.

Chi la possiede:
- non subisce il cooldown

### Regole importanti

- il cooldown è legato al sender
- il timer si aggiorna solo quando il messaggio viene davvero accettato
- un messaggio rifiutato per altri motivi non deve per forza consumare cooldown

### Esempi pratici

Cooldown = 2 secondi

- `ciao` -> passa
- dopo 0.5 secondi `come va` -> bloccato
- dopo 2.1 secondi `come va` -> passa

Caso staff con bypass:
- può inviare messaggi consecutivi senza blocchi

### Accettazione feature

La feature è corretta quando:

- blocca il flood
- rispetta il bypass
- non aggiorna male il timer

---

## Word filter

### Obiettivo

Censurare parole blacklistate in chat e, se previsto, nei PM.

### Matching richiesto

Il matching deve essere almeno:

- case-insensitive
- abbastanza robusto per le forme normali della parola
- non eccessivamente permissivo su parole innocenti

### Output censurato

La parola vietata deve essere sostituita in modo coerente, per esempio:

- simbolo ripetuto a lunghezza originale
- colore dedicato
- eventuale hover con termine originale, se questa funzione è prevista

### Ordine corretto

Il filtro deve lavorare sul testo già sanitizzato.

Esempio:
Input:
`<red>idiota</red>`

Sequenza corretta:
1. sanitizzazione -> `idiota`
2. filtro -> parola censurata
3. rendering finale

### Esempi pratici

Blacklist contiene `idiota`

Messaggio:
`sei un idiota`

Output:
`sei un ******`

Messaggio:
`<red>idiota</red>`

Output finale:
- il termine deve risultare censurato
- il player non deve bypassare il filtro tramite tag

### Accettazione feature

La feature è corretta quando:

- censura in modo coerente
- non si fa aggirare banalmente dai tag
- non rompe il resto del messaggio

---

## ChatItems

### Obiettivo

Permettere ai player di mostrare oggetti o inventari tramite token cliccabili che aprono GUI in sola lettura.

### Token richiesti

Supportare almeno:

- item in mano principale
- preview contenuto shulker quando applicabile
- armor + offhand
- hotbar
- inventario completo
- ender chest

### Regola generale

Quando il player scrive un token valido:

- il plugin crea uno snapshot del contenuto al momento dell’invio
- il token viene renderizzato in modo speciale
- il token diventa cliccabile
- il click apre una GUI view-only coerente con il tipo di preview

### Snapshot obbligatorio

La preview deve mostrare lo stato **al momento del messaggio**, non una view live.

Questo vale per:

- item in mano
- armor
- hotbar
- inventario
- ender chest
- contenuto shulker

### Item in mano

#### Se il player ha un oggetto in mano
- il token diventa preview cliccabile
- chi clicca vede l’item corretto con meta

#### Se il player non ha nulla in mano
- usare fallback sensato
- il messaggio parte comunque
- evitare click inutili o GUI vuote fuorvianti

### Shulker box

Se il token item riguarda una shulker box:

- se il sender ha il permesso adeguato, il token può mostrare la preview contenuto shulker
- la GUI deve essere read-only
- se manca il permesso, non mostrare il contenuto per evitare leak

Fallback accettabili:
- degradare a preview item normale
- non mostrare il contenuto interno
- mantenere il messaggio comunque leggibile

### Armor

La GUI deve mostrare almeno:

- helmet
- chestplate
- leggings
- boots
- offhand

in modo leggibile e stabile.

### Hotbar

Deve mostrare gli slot hotbar del sender al momento del messaggio.

### Inventario completo

Deve mostrare l’inventario completo del sender in view-only.

L’obiettivo è una preview chiara, non necessariamente un clone perfetto dell’interfaccia vanilla in ogni dettaglio.

### Ender chest

Deve mostrare l’ender chest del sender in view-only.

### Permessi

Ogni tipo di ChatItem deve poter avere la propria regola di accesso.

Se il sender non è autorizzato per quel token:

- il messaggio parte comunque
- il token non produce preview cliccabile
- non deve esserci bypass involontario

### Sicurezza GUI

Le GUI ChatItems devono essere realmente view-only.

Chi apre la GUI non deve poter:

- prendere item
- spostare item
- inserire item
- scambiare item con hotbar
- usare drag
- usare number keys
- usare exploit di click inventory

### Dati da preservare

La preview deve preservare il più possibile:

- material
- amount
- display name
- lore
- enchant
- flags rilevanti
- meta rilevante
- contenuto shulker, se consentito

### Esempi pratici

Messaggio:
`vendo questo [item]`

Con diamante in mano:
- token cliccabile
- chi clicca vede il diamante corretto

Messaggio:
`rate my gear [armor]`

- token cliccabile
- si apre GUI armor

Messaggio:
`ho lootato questo [item]`

Item in mano = shulker box piena
- con permesso shulker -> preview contenuto consentita
- senza permesso -> niente leak contenuto

### Accettazione feature

La feature è corretta quando:

- i token funzionano
- le GUI sono davvero read-only
- il contenuto mostrato è uno snapshot
- le regole di accesso separano correttamente le preview

---

## Join, Quit e First Join

### Obiettivo

Gestire i messaggi di entrata e uscita in modo coerente col plugin, con supporto al first join.

### Join

Quando un player entra:

- il plugin può annullare il join vanilla
- invia il join message del plugin, se abilitato

### Quit

Quando un player esce:

- il plugin può annullare il quit vanilla
- invia il quit message del plugin, se abilitato

### First Join

Il first join deve attivarsi solo la prima volta reale in cui il player entra nel server.

#### Regola obbligatoria
- primo accesso reale -> first join message
- accessi successivi -> niente first join

### Contatore univoco opzionale ma desiderato

È utile mantenere un contatore progressivo dei first join unici.

Esempio:
`Benvenuto! Sei il giocatore #152`

Se presente:
- deve essere persistente
- non deve rompersi ai restart

### Priorità tra join normale e first join

Comportamento consigliato:
- il first join sostituisce il join normale, per evitare doppio spam

### Esempi pratici

Primo accesso:
`Benvenuto Steve! Sei il giocatore #42`

Accesso successivo:
`+ Steve`

Non entrambi insieme, salvo scelta diversa esplicita.

### Accettazione feature

La feature è corretta quando:

- vanilla join/quit sono sotto controllo
- first join parte una sola volta
- il contatore, se implementato, è persistente e corretto

---

## Reload

### Obiettivo

Permettere il reload pulito del plugin senza dipendere dal reload globale del server.

### Cosa deve ricaricare

Il reload deve riallineare almeno:

- messaggi
- formati chat
- moduli attivi/disattivi
- custom pings
- rendering mentions
- suoni
- word filter
- token chatitems
- cooldown
- impostazioni generali del plugin

### Cosa non deve fare

Il reload non deve:

- perdere player data persistenti
- duplicare listener
- duplicare logiche runtime
- lasciare stato misto vecchio + nuovo
- rompere le sessioni dei player online

### Feedback

Chi esegue il reload deve ricevere un feedback chiaro:

- successo
- eventuale errore
- idealmente breve riepilogo del reload riuscito

### Accettazione feature

La feature è corretta quando:

- i nuovi valori vengono applicati senza restart
- non nascono comportamenti duplicati
- player data e toggle restano coerenti

---

## Player data

### Obiettivo

Salvare preferenze e stati per-player necessari al plugin.

### Dati minimi richiesti

Almeno:

- ping enabled/disabled
- stato spy, se scelto persistente
- eventuali toggle futuri

### Persistenza

I dati devono sopravvivere a:

- relog
- restart
- reload

### Default richiesti

Default sensati:

- ping -> attivo
- spy -> disattivo

### Lettura dei dati

I player data devono essere letti prima che le feature che ne dipendono vengano applicate.

Esempio:
- il toggle ping deve essere noto prima di inviare notifiche

### Accettazione feature

La feature è corretta quando:

- il player non perde preferenze
- i default sono coerenti
- i dati vengono letti e applicati nel momento corretto

---

## Console logging

### Obiettivo

Fornire log leggibili e utili per amministrazione e debugging.

### Cosa loggare

Almeno:

- chat pubblica
- PM
- broadcast
- reload rilevanti
- errori rilevanti del plugin

### Regole

I log devono essere:

- leggibili
- coerenti
- utili
- non inutilmente spam

### Accettazione feature

La feature è corretta quando:

- la console mostra informazioni utili
- il formato è chiaro
- i log non diventano rumore ingestibile

---

## Casi combinati obbligatori

### Mention + custom ping

Messaggio:
`@help Steve vieni subito in spawn`

Caso:
- sender autorizzato a usare `@help`
- due target online coerenti con `@help`
- Steve è anche menzionato nominalmente

Risultato atteso:
- `@help` evidenziato con il suo stile
- `Steve` evidenziato come mention normale
- tutti i target coerenti ricevono la propria notifica
- Steve riceve una sola notifica totale

### ChatItem + sanitizzazione

Messaggio digitato:
`guarda questo <red>[item]</red>`

Item in mano: diamond sword

Risultato atteso:
- il player non forza il rosso tramite MiniMessage
- il token item viene comunque riconosciuto partendo dal testo pulito
- la preview item è corretta

### Word filter + PM

Messaggio privato:
`sei un idiota`

Risultato atteso:
- PM inviato
- parola censurata secondo policy
- spy vede il formato coerente con la versione moderata scelta dal plugin

### Player con ping disattivati

Messaggio:
`@all Steve venite in arena`

Steve:
- è nei target del custom ping
- è citato nominalmente
- ha ping toggle off

Risultato:
- Steve legge il messaggio in chat
- Steve non riceve suono
- Steve non riceve actionbar
- Steve non riceve doppia notifica, anzi nessuna notifica

### Cooldown + mentions

Caso:
- player è in cooldown
- messaggio contiene `@Steve`

Risultato:
- messaggio bloccato
- Steve non riceve nessuna notifica
- nessuna logica ping deve partire comunque

### ChatItem senza autorizzazione

Messaggio:
`guardate [inventory]`

Caso:
- sender non autorizzato a usare quel token

Risultato:
- messaggio inviato
- niente preview cliccabile
- nessun bypass della restriction

---

## Cose da evitare

Non implementare questi comportamenti:

- hardcode speciale di `@all` e `@help`
- fiducia nell’input MiniMessage del player
- GUI ChatItems modificabili
- notifiche duplicate nello stesso messaggio
- reload che duplica listener o stato runtime
- comportamento network o proxy anche solo “preparato per il futuro” se complica la logica attuale
- overengineering che renda il comportamento meno leggibile

---

## Definizione di completamento del progetto

Considera il progetto implementato correttamente solo se tutte queste condizioni sono vere.

### Comprensione corretta
- il plugin è Paper single-server only
- nessuna logica network è stata introdotta
- il comportamento segue questa specifica, non interpretazioni libere

### Sicurezza e robustezza
- input player sanitizzato
- GUI ChatItems veramente read-only
- regole di accesso rispettate
- fallback sensati
- niente crash in assenza di LuckPerms o PlaceholderAPI

### Coerenza tra feature
- mentions, custom ping e toggle lavorano insieme
- PM, reply e spy sono coerenti
- cooldown blocca davvero prima delle notifiche
- filter lavora sul testo corretto
- reload non crea stato doppio

### Persistenza
- ping toggle persistente
- eventuale spy persistente se scelto
- first join e relativo contatore coerenti
- player data non persi a restart o reload


### File di Configurazione

- Devono essere suddivisi in maniera sensata
- Scrivi commenti sensati dove necessario
- I file default defono essere in lingua inglese

### Verifica pratica minima
Prima di considerare il lavoro finito, verificare almeno questi scenari manuali:

1. chat normale con testo pulito
2. chat con MiniMessage malevolo scritto dal player
3. mention normale valida
4. mention su sottostringa non valida
5. custom ping autorizzato
6. custom ping non autorizzato
7. custom ping con zero target online
8. player con ping toggle off
9. PM + reply
10. PM spy attivo
11. cooldown attivo
12. word filter attivo
13. item preview con item valido
14. item preview a mano vuota
15. inventario completo senza autorizzazione
16. shulker preview con e senza autorizzazione
17. join normale
18. first join
19. reload con valori modificati
20. verifica che il reload non duplichi il comportamento

---

## Istruzione finale per Codex

Implementa il plugin completo seguendo questa specifica come **fonte comportamentale definitiva**.

Quando trovi un’ambiguità:

1. dai priorità ai casi pratici descritti
2. preferisci il comportamento più prevedibile per il player
3. evita automazioni non richieste
4. non aggiungere concetti network o architetture future
5. considera il task finito solo quando i casi normali, i casi limite e i casi combinati descritti qui sono tutti coperti
