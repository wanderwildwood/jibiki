#!/usr/bin/env python3
"""Turn Open English WordNet (WN-LMF XML) into the small SQLite file jibiki ships.

    ./build-db.py english-wordnet-2025.xml.gz wordnet.db

Only what the app shows is kept: words, irregular forms, pronunciation,
senses in their usual order, definitions, examples, and four relations
(antonym between senses; broader, similar and see-also between synsets).
"""
import gzip
import sqlite3
import sys
import xml.etree.ElementTree as ET

POS = {"n": "n", "v": "v", "a": "a", "s": "a", "r": "r"}
SYNSET_RELS = {"hypernym": 1, "instance_hypernym": 1, "similar": 2, "also": 3}


def main(src, dst):
    entries = []      # (lemma, pos, ipa)
    forms = []        # (form, entry index)
    senses = []       # (sense id, entry index, synset id, rank)
    antonyms = []     # (sense id, target sense id)
    synsets = {}      # synset id -> (pos, definition, examples, members)
    synrels = []      # (src synset, type, dst synset)

    with gzip.open(src) if src.endswith(".gz") else open(src, "rb") as f:
        for _, el in ET.iterparse(f, events=("end",)):
            if el.tag == "LexicalEntry":
                lemma = el.find("Lemma")
                text = lemma.get("writtenForm")
                prons = lemma.findall("Pronunciation")
                ipa = next((p.text for p in prons if p.get("variety") in (None, "US")), None) \
                    or (prons[0].text if prons else None)
                idx = len(entries)
                entries.append((text, POS[lemma.get("partOfSpeech")], ipa, el.get("id")))
                for form in el.findall("Form"):
                    forms.append((form.get("writtenForm"), idx))
                for rank, sense in enumerate(el.findall("Sense")):
                    senses.append((sense.get("id"), idx, sense.get("synset"), rank))
                    for rel in sense.findall("SenseRelation"):
                        if rel.get("relType") == "antonym":
                            antonyms.append((sense.get("id"), rel.get("target")))
                el.clear()
            elif el.tag == "Synset":
                definition = el.findtext("Definition") or ""
                examples = "\n".join(e.text for e in el.findall("Example") if e.text)
                synsets[el.get("id")] = (POS[el.get("partOfSpeech")], definition, examples,
                                         el.get("members", "").split())
                for rel in el.findall("SynsetRelation"):
                    kind = SYNSET_RELS.get(rel.get("relType"))
                    if kind:
                        synrels.append((el.get("id"), kind, rel.get("target")))
                el.clear()

    synset_num = {sid: i + 1 for i, sid in enumerate(synsets)}
    entry_num = {e[3]: i + 1 for i, e in enumerate(entries)}
    sense_num = {s[0]: i + 1 for i, s in enumerate(senses)}

    db = sqlite3.connect(dst)
    db.executescript("""
        PRAGMA page_size = 4096;
        CREATE TABLE entry (id INTEGER PRIMARY KEY, lemma TEXT NOT NULL, key TEXT NOT NULL,
                            pos TEXT NOT NULL, ipa TEXT);
        CREATE TABLE form (key TEXT NOT NULL, entry INTEGER NOT NULL);
        CREATE TABLE synset (id INTEGER PRIMARY KEY, pos TEXT NOT NULL,
                             definition TEXT NOT NULL, examples TEXT NOT NULL);
        CREATE TABLE sense (id INTEGER PRIMARY KEY, entry INTEGER NOT NULL,
                            synset INTEGER NOT NULL, rank INTEGER NOT NULL, member INTEGER NOT NULL);
        CREATE TABLE antonym (sense INTEGER NOT NULL, target INTEGER NOT NULL);
        CREATE TABLE relation (synset INTEGER NOT NULL, kind INTEGER NOT NULL, target INTEGER NOT NULL);
    """)
    db.executemany("INSERT INTO entry VALUES (?,?,?,?,?)",
                   ((i + 1, e[0], e[0].lower(), e[1], e[2]) for i, e in enumerate(entries)))
    db.executemany("INSERT INTO form VALUES (?,?)", ((f.lower(), i + 1) for f, i in forms))
    db.executemany("INSERT INTO synset VALUES (?,?,?,?)",
                   ((synset_num[k], v[0], v[1], v[2]) for k, v in synsets.items()))

    def member_rank(entry_idx, synset_id):
        members = synsets[synset_id][3]
        eid = entries[entry_idx][3]
        return members.index(eid) if eid in members else len(members)

    db.executemany("INSERT INTO sense VALUES (?,?,?,?,?)",
                   ((sense_num[s[0]], s[1] + 1, synset_num[s[2]], s[3], member_rank(s[1], s[2]))
                    for s in senses))
    db.executemany("INSERT INTO antonym VALUES (?,?)",
                   ((sense_num[a], sense_num[b]) for a, b in antonyms if b in sense_num))
    db.executemany("INSERT INTO relation VALUES (?,?,?)",
                   ((synset_num[a], k, synset_num[b]) for a, k, b in synrels if b in synset_num))
    db.executescript("""
        CREATE INDEX entry_key ON entry(key);
        CREATE INDEX form_key ON form(key);
        CREATE INDEX sense_entry ON sense(entry);
        CREATE INDEX sense_synset ON sense(synset);
        CREATE INDEX antonym_sense ON antonym(sense);
        CREATE INDEX relation_synset ON relation(synset);
        PRAGMA user_version = 2025;
    """)
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"{len(entries)} entries, {len(senses)} senses, {len(synsets)} synsets, "
          f"{len(forms)} forms, {len(antonyms)} antonyms, {len(synrels)} relations")


if __name__ == "__main__":
    main(*sys.argv[1:3])
