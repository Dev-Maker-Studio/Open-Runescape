#!/usr/bin/env python3
"""Builds lobby/src/main/resources/display-name-vocabulary.txt.

The base list is LDNOOBW (List of Dirty, Naughty, Obscene and Otherwise Bad Words), which is
broad on obscenity but carries plenty of words that are merely crude, and nothing at all on hate
speech - so DROP removes what does not belong in a name filter and SUPPLEMENT adds what it misses.

Whether a term may be matched inside a word is decided against a dictionary rather than by eye: a
term that occurs inside any innocent dictionary word ("ass" in "assassin", "coon" in "raccoon")
can only ever be matched as a whole word.

    ./scripts/build-display-name-vocabulary.py [--ldnoobw <file>] [--dict <file>]
"""

import argparse
import re
import sys
import urllib.request
from pathlib import Path

LDNOOBW_URL = (
    "https://raw.githubusercontent.com/LDNOOBW/"
    "List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/en"
)
DICTIONARY_URL = "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt"
COMMON_URL = "https://raw.githubusercontent.com/first20hours/google-10000-english/master/20k.txt"
MAX_COMMON_CONTAINERS = 0
MIN_SQUEEZED_LENGTH = 4
SUFFIXES = ("", "s", "es", "ed", "d", "ing", "er", "ers", "ist", "ists", "y", "ies", "ness")
OUTPUT = Path("lobby/src/main/resources/display-name-vocabulary.txt")

DROP = {
    "apeshit", "babeland", "ballgag", "bangbros", "bangbus", "bareback", "bdsm", "bbw",
    "bimbos", "birdlock", "bondage", "boner", "busty", "butt", "camgirl", "cialis", "cornhole",
    "dolcett", "dommes", "dryhump", "dvda", "ecchi", "erotic", "erotism", "escort", "femdom",
    "figging", "frotting", "gaysex", "girlon", "goddamn", "gokkun", "goodpoop", "googirl",
    "goregasm", "grope", "gspot", "hardcore", "horny", "hooker", "hotcarl", "humping", "kinbaku",
    "kinkster", "kinky", "livesex", "mong", "mrhands", "nambla", "nawashi", "nipple", "nipples",
    "nsfw", "nude", "nudity", "nutten", "nympho", "omorashi", "orgy", "panties", "panty",
    "pegging", "pikey", "playboy", "ponyplay", "poof", "poon", "poontang", "pubes", "punany",
    "quim", "sadism", "santorum", "sex", "sexcam", "sexo", "sexual", "sexually", "sexy",
    "shibari", "skeet", "snatch", "splooge", "spooge", "spunk", "strapon", "suck", "sucks",
    "tastemy", "tiedup", "topless", "tosser", "tubgirl", "tushy", "twink", "twinkie", "upskirt",
    "viagra", "vibrator", "voyeur", "voyuer", "worldsex", "xx", "xxx", "yaoi", "guro",
}

SUPPLEMENT = {
    "ahegao", "lolicon", "shotacon", "loli", "shota", "futa",
    "necrophile", "necrophilia", "zoophile", "coprophile", "scatophile", "bestial",
    "molest", "pedophile", "pedophilia", "paedophile", "pedofile", "childporn", "gangrap",
    "incest", "prolapse", "smegma", "queef", "felching", "deepthroat", "fleshlight",
    "buttplug", "buttfuck", "assfuck", "dickhead", "cockhead", "dicklet", "cocksuck", "dicksuck",
    "motherfuck", "clusterfuck", "phuck", "jerkoff", "wanker", "wanking", "boobies", "onlyfans",
    "pornhub", "xhamster", "milf", "dilf", "masturbate", "ejaculate", "foreskin", "testicle",
    "scrotum", "sodomy", "hoebag", "skank", "cumslut", "dumbass", "jackass", "shithead",
    "shitface", "dipshit", "bullshit", "arsehole", "butthole", "windowlicker",
    "nazi", "neonazi", "hitler", "siegheil", "heilhitler", "whitepower", "kkk", "reich",
    "gestapo", "holocaust", "jihad", "alqaeda", "lynching",
    "retard", "retarded", "mongoloid", "spaz", "midget", "cripple",
    "gook", "chink", "wop", "dago", "abo", "negro", "negros", "negroes", "mulatto", "halfbreed",
    "sandnigger", "porchmonkey", "tarbaby", "wigger", "niglet", "towelhead",
    "tranny", "trannie", "ladyboy", "dyke", "bulldyke",
    "goatse", "lemonparty", "jailbait", "daterape", "pedobear", "pthc",
    "yiff", "pedo", "paedo", "heil", "arse", "arsed",
}

# Terms whose innocent container the common-word list does not carry ("swanky" for "wank").
FORCE_WHOLE_WORD = {"wank"}

RESERVED = {
    "mod", "mods", "jmod", "jmods", "jagex", "admin", "admins", "administrator", "moderator",
    "owner", "staff", "system", "support", "console",
}

# Words no dictionary knows that nonetheless contain an in-word term, and words that spell one
# when run together with a neighbour ("Niger Red", "Goats Eating").
EXTRA_ALLOW = {
    "niger", "goats", "since", "vince",
    "scunthorpe", "penistone", "shiitake", "shitake", "shiitakes",
    "fagotto", "fagotti", "nazionale", "nazir", "nazim", "naziha",
    "pussycat", "pussycats", "shitepoke",
}


def normalize(text):
    return re.sub(r"[^a-z]", "", text.lower())


def squeeze(text):
    out = []
    for ch in text:
        if not out or out[-1] != ch:
            out.append(ch)
    return "".join(out)


def load(path, url):
    if path:
        return Path(path).read_text(encoding="utf-8").splitlines()
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read().decode("utf-8", "ignore").splitlines()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ldnoobw")
    parser.add_argument("--dict")
    parser.add_argument("--common")
    args = parser.parse_args()

    terms = {normalize(line) for line in load(args.ldnoobw, LDNOOBW_URL)} | SUPPLEMENT
    terms = {t for t in terms if len(t) >= 3} - DROP - RESERVED

    dictionary = {w for w in (normalize(line) for line in load(args.dict, DICTIONARY_URL)) if len(w) >= 3}
    common = {w for w in (normalize(line) for line in load(args.common, COMMON_URL)) if len(w) >= 3}
    inflections = {t + suffix for t in terms for suffix in SUFFIXES}
    innocent = dictionary - terms - inflections

    in_word, whole_word = set(), set()
    for term in sorted(terms):
        containers = {w for w in common - terms - inflections if term in w}
        if term in FORCE_WHOLE_WORD or len(containers) > MAX_COMMON_CONTAINERS:
            whole_word.add(term)
        else:
            in_word.add(term)

    squeezed = {squeeze(t) for t in in_word if len(squeeze(t)) >= MIN_SQUEEZED_LENGTH}
    allow = set(EXTRA_ALLOW) | {
        w for w in innocent
        if any(t in w for t in in_word) or (squeeze(w) != w and any(s in squeeze(w) for s in squeezed))
    }

    sections = [
        ("in-word", in_word),
        ("whole-word", whole_word),
        ("allow", allow),
        ("reserved", RESERVED),
    ]
    lines = [
        "# Display names are matched against this vocabulary.",
        "# Generated by scripts/build-display-name-vocabulary.py - edit that script, not this file.",
        f"# Base list: {LDNOOBW_URL}",
        "# in-word terms match anywhere inside a word; whole-word terms match a whole word only;",
        "# allow exempts a word from in-word matching; reserved is staff impersonation.",
    ]
    for name, values in sections:
        lines.append("")
        lines.append(f"[{name}]")
        lines.extend(sorted(values))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{OUTPUT}: {len(in_word)} in-word, {len(whole_word)} whole-word, {len(allow)} allow", file=sys.stderr)


if __name__ == "__main__":
    main()
