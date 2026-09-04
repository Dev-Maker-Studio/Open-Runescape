package com.jagex.game.runetek5.tools

class LocalizedText(
    english: String,
    german: String,
    french: String,
    portugese: String
) {
    @JvmField val languageMap: MutableMap<Language, String> = HashMap(6)

    init {
        languageMap[Language.ENGLISH] = english
        languageMap[Language.GERMAN] = german
        languageMap[Language.FRENCH] = french
        languageMap[Language.PORTUGESE] = portugese
    }

    fun translate(language: Language): String {
        return languageMap[language] ?: error("Unknown language: $language")
    }

    companion object {
        @JvmField val DEVELOPER_CONSOLE_INTRO = LocalizedText("This is the developer console. To close, press the `, ² or § keys on your keyboard.", "Das ist die Entwicklerkonsole. Zum Schließen, die Tasten `, ² or § drücken.", "Ceci est la console de développement. Pour la fermer, appuyez sur les touches `, ² ou §.", "Este é o painel de controle do desenvolvedor. Para fechar, pressione `, ² ou §.")
        @JvmField val ERROR_EXECUTING_COMMAND = LocalizedText("There was an error executing the command.", "Es gab einen Fehler beim Ausführen des Befehls.", "Une erreur s'est produite lors de l'exécution de la commande.", "Houve um erro quando o comando foi executado.")
        @JvmField val UNKNOWN_DEV_COMMAND = LocalizedText("Unknown developer command: ", "Unbekannter Befehl: ", "Commande inconnue : ", "Comando desconhecido: ")
        @JvmField val CANCEL = LocalizedText("Cancel", "Abbrechen", "Annuler", "Cancelar")
        @JvmField val DISCARD = LocalizedText("Discard", "Ablegen", "Jeter", "Descartar")
        @JvmField val DISCARD_2 = LocalizedText("Discard", "Ablegen", "Jeter", "Descartar")
        @JvmField val TAKE = LocalizedText("Take", "Nehmen", "Prendre", "Pegar")
        @JvmField val DROP = LocalizedText("Drop", "Fallen lassen", "Poser", "Largar")
        @JvmField val CONTINUE = LocalizedText("Continue", "Weiter", "Continuer", "Continuar")
        @JvmField val FRIEND_LIST_FULL = LocalizedText("Your friends list is full (200 names maximum)", "Deine Freunde-Liste ist voll, du hast das Maximum von 200 erreicht.", "Votre liste d'amis est pleine (200 noms maximum).", "Sua lista de amigos está cheia. O limite é 200.")
        @JvmField val LOADING_PLEASE_WAIT = LocalizedText("Loading - please wait.", "Ladevorgang - bitte warte.", "Chargement en cours. Veuillez patienter.", "Carregando. Aguarde.")
        @JvmField val PROFILING = LocalizedText("Profiling...", "Profiling...", "Profilage...", "Profiling...")
        @JvmField val CONNECTION_LOST = LocalizedText("Connection lost.", "Verbindung abgebrochen.", "Connexion perdue.", "Conexão perdida.")
        @JvmField val ATTEMPTING_TO_REESTABLISH = LocalizedText("Please wait - attempting to reestablish.", "Bitte warte - es wird versucht, die Verbindung wiederherzustellen.", "Veuillez patienter - tentative de rétablissement.", "Tentando reestabelecer conexão. Aguarde.")
        @JvmField val CHECKING_FOR_UPDATES = LocalizedText("Checking for updates - ", "Suche nach Updates - ", "Vérification des mises à jour - ", "Verificando atualizações - ")
        @JvmField val FETCHING_UPDATES = LocalizedText("Fetching Updates - ", "Lade Update - ", "Chargement des MAJ - ", "Carregando atualizações - ")
        @JvmField val LOADING = LocalizedText("Loading...", "Lade...", "Chargement en cours...", "Carregando...")
        @JvmField val HAS_LOGGED_IN = LocalizedText(" has logged in.", " loggt sich ein.", " s'est connecté.", " entrou no jogo.")
        @JvmField val HAS_LOGGED_OUT = LocalizedText(" has logged out.", " loggt sich aus.", " s'est déconnecté.", " saiu do jogo.")
        @JvmField val UNABLE_TO_FIND = LocalizedText("Unable to find ", "Spieler kann nicht gefunden werden: ", "Impossible de trouver ", "Não é possível encontrar ")
        @JvmField val EXAMINE = LocalizedText("Examine", "Untersuchen", "Examiner", "Examinar")
        @JvmField val ATTACK = LocalizedText("Attack", "Angreifen", "Attaquer", "Atacar")
        @JvmField val CHOOSE_OPTION = LocalizedText("Choose Option", "Wähl eine Option", "Choisir une option", "Selecionar opção")
        @JvmField val MORE_OPTIONS = LocalizedText(" more options", " weitere Optionen", " autres options", " mais opções")
        @JvmField val WALK_HERE = LocalizedText("Walk here", "Hierhin gehen", "Atteindre", "Caminhar para cá")
        @JvmField val FACE_HERE = LocalizedText("Face here", "Hierhin drehen", "Regarder dans cette direction", "Virar para cá")
        @JvmField val MINIMENU_LEVEL = LocalizedText("level: ", "Stufe: ", "niveau ", "nível: ")
        @JvmField val SKILL_ = LocalizedText("skill: ", "Fertigkeit: ", "compétence ", "habilidade: ")
        @JvmField val RATING_ = LocalizedText("rating: ", "Kampfstufe: ", "classement ", "qualificação: ")
        @JvmField val PLEASE_WAIT = LocalizedText("Please wait...", "Bitte warte...", "Veuillez attendre", "Aguarde...")
        @JvmField val DELIMITER = LocalizedText(" ", ": ", " ", " ")
        @JvmField val M = LocalizedText("M", "M", "M", "M")
        @JvmField val M2 = LocalizedText("M", "M", "M", "M")
        @JvmField val K = LocalizedText("K", "T", "K", "K")
        @JvmField val K2 = LocalizedText("K", "T", "K", "K")
        @JvmField val SELF = LocalizedText("Self", "Mich", "Moi", "Eu")
        @JvmField val ALREADY_ON_FRIENDS_LIST = LocalizedText(" is already on your friends list.", " steht bereits auf deiner Freunde-Liste!", " est déjà dans votre liste d'amis.", " já está na sua lista de amigos.")
        @JvmField val IGNORE_LIST_FULL = LocalizedText("Your ignore list is full. Max of 100 users.", "Deine Ignorieren-Liste ist voll, du kannst nur 100 Spieler darauf eintragen.", "Votre liste noire est pleine (100 noms maximum).", "Sua lista de ignorados está cheia. O limite é 100 usuários.")
        @JvmField val ALREADY_IGNORED = LocalizedText(" is already on your ignore list.", " steht bereits auf deiner Ignorieren-Liste!", " est déjà dans votre liste noire.", " já está na sua lista de ignorados.")
        @JvmField val CANT_ADD_SELF_TO_FRIEND = LocalizedText("You can't add yourself to your own friends list.", "Du kannst dich nicht auf deine eigene Freunde-Liste setzen!", "Vous ne pouvez pas ajouter votre nom à votre liste d'amis.", "Você não pode adicionar a si próprio à sua lista de amigos.")
        @JvmField val CANT_ADD_SELF_TO_IGNORE = LocalizedText("You can't add yourself to your own ignore list.", "Du kannst dich nicht auf deine eigene Ignorieren-Liste setzen!", "Vous ne pouvez pas ajouter votre nom à votre liste noire.", "Você não pode adicionar a si próprio à sua lista de ignorados.")
        @JvmField val PLEASE_REMOVE_IGNORE = LocalizedText("Please remove ", "Bitte entferne ", "Veuillez commencer par supprimer ", "Remova ")
        @JvmField val FROM_IGNORE_FIRST = LocalizedText(" from your ignore list first.", " zuerst von deiner Ignorieren-Liste!", " de votre liste noire.", " da sua lista de ignorados primeiro.")
        @JvmField val PLEASE_REMOVE = LocalizedText("Please remove ", "Bitte entferne ", "Veuillez commencer par supprimer ", "Remova ")
        @JvmField val FROM_FRIENDS_FIRST = LocalizedText(" from your friends list first.", " zuerst von deiner Freunde-Liste!", " de votre liste d'amis.", " da sua lista de amigos primeiro.")
        @JvmField val YELLOW = LocalizedText("yellow:", "gelb:", "jaune:", "amarelo:")
        @JvmField val RED = LocalizedText("red:", "rot:", "rouge:", "vermelho:")
        @JvmField val GREEN = LocalizedText("green:", "grün:", "vert:", "verde:")
        @JvmField val CYAN = LocalizedText("cyan:", "blaugrün:", "cyan:", "cyan:")
        @JvmField val PURPLE = LocalizedText("purple:", "lila:", "violet:", "roxo:")
        @JvmField val WHITE = LocalizedText("white:", "weiss:", "blanc:", "branco:")
        @JvmField val FLASH1 = LocalizedText("flash1:", "blinken1:", "clignotant1:", "flash1:")
        @JvmField val FLASH2 = LocalizedText("flash2:", "blinken2:", "clignotant2:", "flash2:")
        @JvmField val FLASH3 = LocalizedText("flash3:", "blinken3:", "clignotant3:", "brilho3:")
        @JvmField val GLOW1 = LocalizedText("glow1:", "leuchten1:", "brillant1:", "brilho1:")
        @JvmField val GLOW2 = LocalizedText("glow2:", "leuchten2:", "brillant2:", "brilho2:")
        @JvmField val GLOW3 = LocalizedText("glow3:", "leuchten3:", "brillant3:", "brilho3:")
        @JvmField val WAVE = LocalizedText("wave:", "welle:", "ondulation:", "onda:")
        @JvmField val WAVE2 = LocalizedText("wave2:", "welle2:", "ondulation2:", "onda2:")
        @JvmField val SHAKE = LocalizedText("shake:", "schütteln:", "tremblement:", "tremor:")
        @JvmField val SCROLL = LocalizedText("scroll:", "scrollen:", "déroulement:", "rolagem:")
        @JvmField val SLIDE = LocalizedText("slide:", "gleiten:", "glissement:", "deslizamento:")
    }
}
