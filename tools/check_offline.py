"""Offline verification of actual DDL, Java syntax/rules, and resource references.
Requires Python 3 and a JDK (tested with JDK 17). Does not build or run Android UI.
Run from project root: python tools/check_offline.py
"""
import json, re, sqlite3, subprocess, tempfile, shutil
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/example/sqlitek24dtcn533'
RES=ROOT/'app/src/main/res'
source=(SRC/'DatabaseHelper.java').read_text(encoding='utf-8')
create=source.split('public void onCreate(SQLiteDatabase db) {',1)[1].split('@Override public void onUpgrade',1)[0]
statements=[]
for expression in re.findall(r'db\.execSQL\((.*?)\);',create,re.S):
    statements.append(''.join(json.loads(token) for token in re.findall(r'"(?:\\.|[^"\\])*"',expression)))
assert len(statements)==8, len(statements)
db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
for sql in statements:db.execute(sql)
count=0
def check(condition):
    global count
    assert condition
    count+=1
def rejected(sql,args=()):
    global count
    try:db.execute(sql,args)
    except sqlite3.IntegrityError:count+=1;return
    raise AssertionError('Constraint did not reject: '+sql)
vehicle="INSERT INTO vehicles(code,plate,plate_key,brand,brand_key,status,search_key) VALUES (?,?,?,?,?,?,?)"
db.execute(vehicle,('XE001','29B1-123.45','29B112345','Honda','honda','MAINTENANCE','xe001 29b1-123.45 29b112345 honda'))
db.execute(vehicle,('XE002','30F1-678.90','30F167890','HONDA','honda','NORMAL','xe002 30f1-678.90 30f167890 honda'))
db.execute(vehicle,('XE003','29C1-246.80','29C124680','Yamaha','yamaha','MAINTENANCE','xe003 yamaha'))
rejected(vehicle,('xe001','29A1-111.11','29A111111','Suzuki','suzuki','NORMAL',''))
rejected(vehicle,('XE004','29B1 12345','29B112345','Honda','honda','NORMAL',''))
rejected(vehicle,('XE004','29A1-111.11','29A111111','Honda','honda','UNKNOWN',''))
check(db.execute("SELECT COUNT(*) FROM vehicles WHERE brand_key=?",('honda',)).fetchone()[0]==2)
check(db.execute("SELECT COUNT(*) FROM vehicles WHERE status='MAINTENANCE'").fetchone()[0]==2)
check(db.execute("SELECT COUNT(*) FROM vehicles WHERE brand_key=? AND status=?",('honda','MAINTENANCE')).fetchone()[0]==1)
check(db.execute("SELECT COUNT(*) FROM vehicles WHERE instr(search_key,?)>0",("' OR 1=1 --",)).fetchone()[0]==0)
db.execute("INSERT INTO services(code,name,price,search_key) VALUES ('DV001','Thay dầu',120000,'dv001 thay dau')")
rejected("INSERT INTO services(code,name,price,search_key) VALUES ('dv001','A',1,'a')")
rejected("INSERT INTO services(code,name,price,search_key) VALUES ('DV002','A',-1,'a')")
rejected("INSERT INTO services(code,name,price,search_key) VALUES ('DV002','A',1000000001,'a')")
assign="INSERT INTO repair_orders(vehicle_id,service_id,service_code,service_name,price,assigned_at) SELECT ?,id,code,name,price,1000 FROM services WHERE id=?"
db.execute(assign,(1,1));rejected(assign,(1,1));rejected(assign,(999,1))
rejected('DELETE FROM vehicles WHERE id=1');rejected('DELETE FROM services WHERE id=1')
db.execute("UPDATE services SET price=150000,name='Thay dầu mới' WHERE id=1")
check(db.execute('SELECT price,service_name FROM repair_orders WHERE id=1').fetchone()==(120000,'Thay dầu'))
rejected("UPDATE repair_orders SET status='COMPLETED' WHERE id=1")
db.execute("UPDATE repair_orders SET status='COMPLETED',completed_at=2000 WHERE id=1")
db.execute(assign,(1,1))
check(db.execute('SELECT price FROM repair_orders ORDER BY id DESC LIMIT 1').fetchone()[0]==150000)
check(db.execute('SELECT status FROM vehicles WHERE id=1').fetchone()[0]=='MAINTENANCE')
db.execute('DELETE FROM repair_orders WHERE vehicle_id=1')
db.execute('DELETE FROM vehicles WHERE id=1');db.execute('DELETE FROM services WHERE id=1')
check(db.execute('SELECT COUNT(*) FROM vehicles').fetchone()[0]==2)
print(f'{count} SQLite checks passed using DDL extracted from DatabaseHelper.java.')
# All XML well-formed; app-defined references resolve.
xml_files=list((ROOT/'app/src').rglob('*.xml'))
for path in xml_files:ET.parse(path)
resources={}
for path in RES.rglob('*'):
    if not path.is_file():continue
    group=path.parent.name.split('-')[0]
    if group!='values':resources.setdefault(group,set()).add(path.stem)
    elif path.suffix=='.xml':
        for child in ET.parse(path).getroot():
            name=child.get('name')
            if name:resources.setdefault(child.tag,set()).add(name)
all_xml='\n'.join(p.read_text(encoding='utf-8') for p in xml_files)
resources['id']=set(re.findall(r'@\+id/([A-Za-z_0-9]+)',all_xml))
for group,name in re.findall(r'@(?:\+)?([a-z]+)/([A-Za-z_0-9.]+)',all_xml):
    assert name in resources.get(group,set()),(group,name)
for path in SRC.glob('*.java'):
    for group,name in re.findall(r'(?<!android\.)\bR\.(\w+)\.(\w+)',path.read_text(encoding='utf-8')):
        assert name in resources.get(group,set()),(path.name,group,name)
manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
for activity in manifest.findall('.//activity'):
    name=activity.get('{http://schemas.android.com/apk/res/android}name').lstrip('.')
    assert (SRC/(name+'.java')).exists(),name
print(f'{len(xml_files)} XML files parsed; resource references and manifest activities resolved.')
compiler=['javac'] if shutil.which('javac') else ['java','-m','jdk.compiler/com.sun.tools.javac.Main']
with tempfile.TemporaryDirectory() as temp:
    subprocess.run([*compiler,'-encoding','UTF-8','-d',temp,str(ROOT/'tools/CheckJava.java')],check=True)
    subprocess.run(['java','-cp',temp,'CheckJava',str(ROOT/'app/src')],check=True)
    pure=['InputRules.java','Vehicle.java','ServiceItem.java','RepairOrder.java']
    subprocess.run([*compiler,'--release','11','-encoding','UTF-8','-d',temp,*[str(SRC/p) for p in pure],str(ROOT/'tools/RulesSmoke.java')],check=True)
    subprocess.run(['java','-cp',temp,'RulesSmoke'],check=True)
print('Offline checks passed. Android Gradle build and device tests must still be run in Android Studio.')
