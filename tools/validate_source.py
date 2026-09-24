"""Host-side checks only. This does not compile Kotlin or run Android."""
from pathlib import Path
import re, sqlite3, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
res=root/'app/src/main/res'
for f in res.rglob('*.xml'): ET.parse(f)
ET.parse(root/'app/src/main/AndroidManifest.xml')
known={}
for f in res.rglob('*.xml'):
 if f.parent.name=='values':
  for item in ET.parse(f).getroot():
   if item.get('name'): known.setdefault(item.tag,set()).add(item.get('name'))
 else: known.setdefault(f.parent.name,set()).add(f.stem)
for f in res.rglob('*.xml'):
 for kind,name in re.findall(r'@(?:\+)?(layout|drawable|color|menu)/([\w.]+)',f.read_text()): assert name in known[kind], (f,kind,name)
source=(root/'app/src/main/java/com/codearc/app/data/CodeArcDatabase.kt').read_text()
sql=re.search(r'db.execSQL\("([^"]+)"\)',source).group(1)
db=sqlite3.connect(':memory:')
db.execute('CREATE TABLE app_metadata (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)')
db.execute("INSERT INTO app_metadata VALUES ('foundation_version','1')")
db.execute(sql)
assert db.execute('SELECT value FROM app_metadata').fetchone()==('1',)
columns={row[1] for row in db.execute('PRAGMA table_info(projects)')}
assert columns=={'id','name','language','path','created','modified','favorite','executionMode','mainFile','runtimePreference'}
db.execute("INSERT INTO projects VALUES ('id','Calculator','Python','/private/id',1,2,0,'Automatic','src/main.py','Default')")
assert db.execute('SELECT name FROM projects').fetchone()==('Calculator',)
print('PASS: XML parsing, local resource references, migration SQL, Phase 1 metadata preservation, project column coverage and SQL insertion.')
print('NOT RUN: Kotlin compilation, Room-generated migration validation, JUnit tests, APK/device tests.')
