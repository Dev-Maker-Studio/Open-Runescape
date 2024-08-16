package com.jagex.game.runetek5.tools;

import java.util.HashMap;
import java.util.Map;

public class LocalizedText {

    public static final LocalizedText DEVELOPER_CONSOLE_INTRO = new LocalizedText("This is the developer console. To close, press the `, ² or § keys on your keyboard.", "Das ist die Entwicklerkonsole. Zum Schließen, die Tasten `, ² or § drücken.", "Ceci est la console de développement. Pour la fermer, appuyez sur les touches `, ² ou §.", "Este é o painel de controle do desenvolvedor. Para fechar, pressione `, ² ou §.");
    public static final LocalizedText ERROR_EXECUTING_COMMAND = new LocalizedText("There was an error executing the command.", "Es gab einen Fehler beim Ausführen des Befehls.", "Une erreur s'est produite lors de l'exécution de la commande.", "Houve um erro quando o comando foi executado.");
    public static final LocalizedText UNKNOWN_DEV_COMMAND = new LocalizedText("Unknown developer command: ", "Unbekannter Befehl: ", "Commande inconnue : ", "Comando desconhecido: ");
    public static final LocalizedText CANCEL = new LocalizedText("Cancel", "Abbrechen", "Annuler", "Cancelar");
    public static final LocalizedText DISCARD = new LocalizedText("Discard", "Ablegen", "Jeter", "Descartar");
    public static final LocalizedText DISCARD_2 = new LocalizedText("Discard", "Ablegen", "Jeter", "Descartar");
    public static final LocalizedText TAKE = new LocalizedText("Take", "Nehmen", "Prendre", "Pegar");
    public static final LocalizedText DROP = new LocalizedText("Drop", "Fallen lassen", "Poser", "Largar");
    public static final LocalizedText CONTINUE = new LocalizedText("Continue", "Weiter", "Continuer", "Continuar");
    public static final LocalizedText FRIEND_LIST_FULL = new LocalizedText("Your friends list is full (200 names maximum)", "Deine Freunde-Liste ist voll, du hast das Maximum von 200 erreicht.", "Votre liste d'amis est pleine (200 noms maximum).", "Sua lista de amigos está cheia. O limite é 200.");
    public static final LocalizedText LOADING_PLEASE_WAIT = new LocalizedText("Loading - please wait.", "Ladevorgang - bitte warte.", "Chargement en cours. Veuillez patienter.", "Carregando. Aguarde.");
    public static final LocalizedText PROFILING = new LocalizedText("Profiling...", "Profiling...", "Profilage...", "Profiling...");
    public static final LocalizedText CONNECTION_LOST = new LocalizedText("Connection lost.", "Verbindung abgebrochen.", "Connexion perdue.", "Conexão perdida.");
    public static final LocalizedText ATTEMPTING_TO_REESTABLISH = new LocalizedText("Please wait - attempting to reestablish.", "Bitte warte - es wird versucht, die Verbindung wiederherzustellen.", "Veuillez patienter - tentative de rétablissement.", "Tentando reestabelecer conexão. Aguarde.");
    public static final LocalizedText CHECKING_FOR_UPDATES = new LocalizedText("Checking for updates - ", "Suche nach Updates - ", "Vérification des mises à jour - ", "Verificando atualizações - ");
    public static final LocalizedText FETCHING_UPDATES = new LocalizedText("Fetching Updates - ", "Lade Update - ", "Chargement des MAJ - ", "Carregando atualizações - ");
    public static final LocalizedText LOADING = new LocalizedText("Loading...", "Lade...", "Chargement en cours...", "Carregando...");
    public static final LocalizedText HAS_LOGGED_IN = new LocalizedText(" has logged in.", " loggt sich ein.", " s'est connecté.", " entrou no jogo.");
    public static final LocalizedText HAS_LOGGED_OUT = new LocalizedText(" has logged out.", " loggt sich aus.", " s'est déconnecté.", " saiu do jogo.");
    public static final LocalizedText UNABLE_TO_FIND = new LocalizedText("Unable to find ", "Spieler kann nicht gefunden werden: ", "Impossible de trouver ", "Não é possível encontrar ");
    public static final LocalizedText EXAMINE = new LocalizedText("Examine", "Untersuchen", "Examiner", "Examinar");
    public static final LocalizedText ATTACK = new LocalizedText("Attack", "Angreifen", "Attaquer", "Atacar");
    public static final LocalizedText CHOOSE_OPTION = new LocalizedText("Choose Option", "Wähl eine Option", "Choisir une option", "Selecionar opção");
    public static final LocalizedText MORE_OPTIONS = new LocalizedText(" more options", " weitere Optionen", " autres options", " mais opções");
    public static final LocalizedText WALK_HERE = new LocalizedText("Walk here", "Hierhin gehen", "Atteindre", "Caminhar para cá");
    public static final LocalizedText FACE_HERE = new LocalizedText("Face here", "Hierhin drehen", "Regarder dans cette direction", "Virar para cá");
    public static final LocalizedText MINIMENU_LEVEL = new LocalizedText("level: ", "Stufe: ", "niveau ", "nível: ");
    public static final LocalizedText SKILL_ = new LocalizedText("skill: ", "Fertigkeit: ", "compétence ", "habilidade: ");
    public static final LocalizedText RATING_ = new LocalizedText("rating: ", "Kampfstufe: ", "classement ", "qualificação: ");
    public static final LocalizedText PLEASE_WAIT = new LocalizedText("Please wait...", "Bitte warte...", "Veuillez attendre", "Aguarde...");
    public static final LocalizedText DELIMITER = new LocalizedText(" ", ": ", " ", " ");
    public static final LocalizedText M = new LocalizedText("M", "M", "M", "M");
    public static final LocalizedText M2 = new LocalizedText("M", "M", "M", "M");
    public static final LocalizedText K = new LocalizedText("K", "T", "K", "K");
    public static final LocalizedText K2 = new LocalizedText("K", "T", "K", "K");
    public static final LocalizedText SELF = new LocalizedText("Self", "Mich", "Moi", "Eu");
    public static final LocalizedText ALREADY_ON_FRIENDS_LIST = new LocalizedText(" is already on your friends list.", " steht bereits auf deiner Freunde-Liste!", " est déjà dans votre liste d'amis.", " já está na sua lista de amigos.");
    public static final LocalizedText IGNORE_LIST_FULL = new LocalizedText("Your ignore list is full. Max of 100 users.", "Deine Ignorieren-Liste ist voll, du kannst nur 100 Spieler darauf eintragen.", "Votre liste noire est pleine (100 noms maximum).", "Sua lista de ignorados está cheia. O limite é 100 usuários.");
    public static final LocalizedText ALREADY_IGNORED = new LocalizedText(" is already on your ignore list.", " steht bereits auf deiner Ignorieren-Liste!", " est déjà dans votre liste noire.", " já está na sua lista de ignorados.");
    public static final LocalizedText CANT_ADD_SELF_TO_FRIEND = new LocalizedText("You can't add yourself to your own friends list.", "Du kannst dich nicht auf deine eigene Freunde-Liste setzen!", "Vous ne pouvez pas ajouter votre nom à votre liste d'amis.", "Você não pode adicionar a si próprio à sua lista de amigos.");
    public static final LocalizedText CANT_ADD_SELF_TO_IGNORE = new LocalizedText("You can't add yourself to your own ignore list.", "Du kannst dich nicht auf deine eigene Ignorieren-Liste setzen!", "Vous ne pouvez pas ajouter votre nom à votre liste noire.", "Você não pode adicionar a si próprio à sua lista de ignorados.");
    public static final LocalizedText PLEASE_REMOVE_IGNORE = new LocalizedText("Please remove ", "Bitte entferne ", "Veuillez commencer par supprimer ", "Remova ");
    public static final LocalizedText FROM_IGNORE_FIRST = new LocalizedText(" from your ignore list first.", " zuerst von deiner Ignorieren-Liste!", " de votre liste noire.", " da sua lista de ignorados primeiro.");
    public static final LocalizedText PLEASE_REMOVE = new LocalizedText("Please remove ", "Bitte entferne ", "Veuillez commencer par supprimer ", "Remova ");
    public static final LocalizedText FROM_FRIENDS_FIRST = new LocalizedText(" from your friends list first.", " zuerst von deiner Freunde-Liste!", " de votre liste d'amis.", " da sua lista de amigos primeiro.");
    public static final LocalizedText YELLOW = new LocalizedText("yellow:", "gelb:", "jaune:", "amarelo:");
    public static final LocalizedText RED = new LocalizedText("red:", "rot:", "rouge:", "vermelho:");
    public static final LocalizedText GREEN = new LocalizedText("green:", "grün:", "vert:", "verde:");
    public static final LocalizedText CYAN = new LocalizedText("cyan:", "blaugrün:", "cyan:", "cyan:");
    public static final LocalizedText PURPLE = new LocalizedText("purple:", "lila:", "violet:", "roxo:");
    public static final LocalizedText WHITE = new LocalizedText("white:", "weiss:", "blanc:", "branco:");
    public static final LocalizedText FLASH1 = new LocalizedText("flash1:", "blinken1:", "clignotant1:", "flash1:");
    public static final LocalizedText FLASH2 = new LocalizedText("flash2:", "blinken2:", "clignotant2:", "flash2:");
    public static final LocalizedText FLASH3 = new LocalizedText("flash3:", "blinken3:", "clignotant3:", "brilho3:");
    public static final LocalizedText GLOW1 = new LocalizedText("glow1:", "leuchten1:", "brillant1:", "brilho1:");
    public static final LocalizedText GLOW2 = new LocalizedText("glow2:", "leuchten2:", "brillant2:", "brilho2:");
    public static final LocalizedText GLOW3 = new LocalizedText("glow3:", "leuchten3:", "brillant3:", "brilho3:");
    public static final LocalizedText WAVE = new LocalizedText("wave:", "welle:", "ondulation:", "onda:");
    public static final LocalizedText WAVE2 = new LocalizedText("wave2:", "welle2:", "ondulation2:", "onda2:");
    public static final LocalizedText SHAKE = new LocalizedText("shake:", "schütteln:", "tremblement:", "tremor:");
    public static final LocalizedText SCROLL = new LocalizedText("scroll:", "scrollen:", "déroulement:", "rolagem:");
    public static final LocalizedText SLIDE = new LocalizedText("slide:", "gleiten:", "glissement:", "deslizamento:");

    public final Map<Language, String> languageMap = new HashMap<Language, String>(6);

    public LocalizedText(String english, String german, String french, String portugese) {
        this.languageMap.put(Language.ENGLISH, english);
        this.languageMap.put(Language.GERMAN, german);
        this.languageMap.put(Language.FRENCH, french);
        this.languageMap.put(Language.PORTUGESE, portugese);
    }

    public String translate(Language language) {
        return this.languageMap.get(language);
    }
}
