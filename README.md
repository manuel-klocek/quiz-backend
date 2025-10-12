# Projektarbeit Web Engineering: Backend
#### Bearbeitet von Manuel Klocek

## Installationsbeschreibung

Das Projekt ist containerbasiert aufgebaut und benötigt daher Docker.

Zum Starten des Projekts öffnen Sie bitte ein Terminal im Projektpfad und starten
Datenbank-Container über folgenden Befehl:

__docker-compose up -d__

Als Nächstes können Sie das Backend über mehrere Arten starten:

- Starten in der IDE per Ausführen der Main-Methode in Application.kt (Intellij IDEA)
- Ausführen der quiz-backend-all.jar im Verzeichnis: build/libs

HINWEIS: Das Starten des Backends kann etwas dauern, da erst die Fragendatenbank befüllt werden muss.
Dies dauert etwa 8,5 Minuten

---

Sollte das Backend lokal keinen Build zulassen ist dies kein Problem, da ich den Build-Ordner mit einem erfolgreichen Build mitgebe.

---

Zum Testen des Backends können Sie Ihrer Wahl nach bspw. Postman nutzen, ich biete Ihnen
jedoch mein vorhandenes Frontend an.

Starten des Frontends:

Benötigt wird node und pnpm. Zum Installieren von Pnpm führen Sie folgenden Befehl aus: __npm install -g pnpm__

Als nächstes Installieren Sie die dependencies (im Projektpfad) mit: __pnpm install__

Starten des Frontends: __pnpm run dev__

## Beschreibung von Businesmodell, Prozessen und Systemlandschaft

Das Projekt soll eine Quiz-Applikation mit voll funktionsfähigem Frontend und Backend umfassen.
Dabei besteht das Projekt aus ingesamt 3 Teilen, welche bereit zum Deployment auf die Cloud sind. 
Die Systemlandschaft setzt sich daher aus einem Frontend, also der Benutzeroberfläche der Applikation, 
einem Backend, also die Verwaltung des Spielgeschehens und weiterer Funktionen, und der Datenbank zur Abspeicherung
der Fragen und Highscores, zusammen.

Dabei sollen bereits vorhandene Fragen aus der Open TriviaDB gesammelt werden und zur Nachverfolgung der bereits gespielten Fragen,
selbst gespeichert und verwaltet werden.

## Beschreibung von 5 Kernkonzepten in der Anwendung

### 1. Authentifizierung

Die Authentifizierung wird mittels, des vom Ktor-Framework angebotenen, Plugins __Security__ im Backend umgesetzt.

Dabei besteht die Authentifizierung aus zwei Teilen. Zur Anmeldung nutzen wir die Basic-Authentication
die mithilfe von einem Nutzernamen und Passwort funktioniert. Ist der Nutzer hinterlegt und der Abgleich
der Daten ist erfolgreich, so erhält dieser als Antwort einen JSON-Web-Token (JWT). Dieser Token wird als Session-Token 
für alle künftigen Anfragen genutzt. Somit werden die Anmeldedaten nie im Frontend gespeichert und seltenst 
möglich übertragen. Die Token haben nur eine gewisse Lebensdauer und werden für jeden User einzeln kreiert.

### 2. Hintergrundprozesse - Coroutines

Um das Backend auch auf einer Cloud funktionsfähig zu machen ist es notwendig die Datenbankbefüllung der Fragen als 
Hintergrundaufgabe bzw. als Coroutine zu starten. Andernfalls würde das Backend bis zum Abschluss der Synchronisation 
keinen Health-Check, beim Deployment bzw. starten in der Cloud, beantworten können und würde daher Timeouten. 

Ebenfalls bieten sich hier Coroutines an, da ein Request auf die Open TriviaDB nur alle 5 Sekunden erfolgen darf und
daher ein Delay zwischen Anfragen gesetzt werden muss.

Für die Coroutines nutze ich das von Kotlin angebotene Modul __kotlinx.coroutines__.

### 3. Datenbank

Zur Abspeicherung von Nutzer- und Fragendaten wird eine Datenbank eingesetzt. Hierbei setze ich
der Einfachheit halber auf __MongoDB__. Dies ist eine NOSQL-Datenbank, die keiner klassische relationellen
Datenbank entspricht. Hier können ganze Objekte auf einmal gespeichert und verwalten werden.

Das Aufsetzen der Datenbank geschieht durch die Definition der Objekte, bzw. der Definition von DTO's. Diese
__Data Transfer Objects__ kreieren die Datenbankstruktur automatisch bei der ersten Abspeicherung.

### 4. Sichere Abspeicherung von Nutzerpasswörtern

Zur Sicherstellung der Passwörter gegen Datenklau werden diese verschlüsselt. Die Verschlüsselung basiert
auf einem Hashen der Klarpasswörter mit einem Secret, welches Autonom beim Programmstart generiert wird. Die
Datenbank speichert immer nur das gehashte Passwort. 

Um das Rückschließen von u.U. entwendeten verschlüsselten Passwörter zu vermeiden, wird ein Salt eingesetzt welcher
dafür sorgt, dass selbst 2 identische Passwörter unterschiedliche Hashes ergeben.

### 5. Caching

Da der Browser über die Anzeige von Webseiten hinweg noch viele weitere Funktionen verfügt, 
können die vom Backend übertragenen Daten eingesehen werden. Sollten also die Fragen samt ihrer richtigen Antwort
übertragen werden, so kann dies über einfache Schritte ausgelesen und betrogen werden. Um dies zu Vermeiden, werden die
Fragen und Antwortmöglichkeiten einzeln übertragen und die Antwort des Nutzers zur Validierung an das Backend gesendet.
Das Backend antwortet dann mit der korrekten Antwort zur Feedbackrückgabe an den Nutzer und der nächsten Frage. 

Damit das Backend die Punkteberechnung und Antwortvalidierung fortlaufend über mehrere Nutzer gleichzeitig bewerkstelligen kann,
braucht es ein Caching. Dieses Caching sollte gegenüber der Hauptdatenbank eine bessere Performance besitzen, wird jedoch 
hier nur als weiteres Schema betrachtet, da dies keiner Skalierung benötigt.

## Einordnung weiterer Vorlesungsbestandteile

### 1. Cloud-Computing

Das Projekt wurde bereits für einen gewissen Zeitraum auf unserer hauseigenen Cloud (StackIT) betrieben. Dafür war die 
Entwicklung der manifest-Datei, die Auseinandersetzung mit der Dokumentation und die Anwendung des theoretisch erlernten Wissens 
über das Cloud-Computing notwendig.

### 2. Client-Side vs Server-Side

Wie beim Caching schon beschrieben konnte sich herauskristallisieren, dass die Antwortvalidierung in verfrühten Versionen im Frontend
stattgefunden hat. Um Betrug zu vermeiden, musste daher auf die Server-Side Validierung gewechselt werden, wodurch der Theorie ein 
deutlicher Praxisbezug geschaffen werden konnte.

### 3. Containerisierung

Für ein Cloud-Deployment und die lokale Entwicklung eignet sich die Containerisierung sehr. Dies ist der Fall, da bei einem Deployment
im Normalfall ebenfalls Container genutzt werden und damit keine großen Schwierigkeiten von der lokalen Entwicklungsumgebung bis in 
die Cloud herrschen. Auch wird eine Übersichtlichkeit über unterschiedliche Projekt geschaffen.

### 4. Cross-Origin Resource Sharing

Da das Backend sowohl lokal als auch extern auf einer anderen Netzwerk-, bzw. Webadresse betrieben wird muss CORS freigegeben werden.
Dies ist ein Sicherheitsmechanismus der es ermöglicht, dass das Frontend nur von definierten und der eigenen Adresse Daten entgegennimmt.

### 5. Exception-Handling

Damit der Nutzer auch bei Problemen im Backend die Applikation weiterhin nutzen kann, ist ein Exception-Handling
unumgänglich. Fehler die durch falsche Eingaben oder Bugs entstehen müssen aufgefangen und behandelt werden. Entweder
muss auf einen Fehler danach durch die Software reagiert werden, oder der Nutzer kann es einfach erneut versuchen. In jedem Fall
muss ein Feedback durch das Backend an das Frontend gegeben werden und durch dieses dem Nutzer angezeigt werden. 
