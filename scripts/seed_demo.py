
import sqlite3, time
db = r"D:\Documents\deepseek-harness\default-workspace\cooking-timer\shots\seed.db"
c = sqlite3.connect(db)
c.execute("PRAGMA journal_mode=DELETE")
tables = [r[0] for r in c.execute("select name from sqlite_master where type='table'")]
print("tables:", tables)
now = int(time.time()*1000)
recipes = [("红烧肉","经典本帮，肥而不腻"), ("番茄牛腩","酸甜浓郁，拌饭一绝"), ("银耳羹","小火慢炖出胶")]
ids = []
for name, note in recipes:
    cur = c.execute("INSERT INTO recipes(name,note,is_builtin,created_at,updated_at) VALUES(?,?,?,?,?)", (name,note,0,now,now))
    ids.append(cur.lastrowid)
steps = {
 0: [("五花肉切块焯水",5*60,"冷水下锅，加姜片料酒"), ("炒糖色",10*60,"小火慢炒，别糊了"), ("加水炖煮",40*60,"没过肉面两指"), ("大火收汁",5*60,"翻动防粘底")],
 1: [("牛腩焯水",8*60,""), ("番茄炒出沙",10*60,"加少许糖"), ("炖煮入味",60*60,"小火"), ("收汁调味",8*60,"")],
 2: [("泡发银耳",20*60,"提前两小时冷水泡"), ("小火慢炖",40*60,"加冰糖"), ("加枸杞",3*60,"")],
}
for ri, rid in enumerate(ids):
    for oi, (t, d, n) in enumerate(steps[ri]):
        c.execute("INSERT INTO steps(recipe_id,order_no,title,duration_sec,note) VALUES(?,?,?,?,?)", (rid,oi,t,d,n))

# 实例：红烧肉 #1（进行中），银耳羹 #2
def make_cook(rid, label, status, done_upto, running_idx):
    cur = c.execute("INSERT INTO cooks(recipe_id,label,status,created_at,finished_at) VALUES(?,?,?,?,NULL)", (rid,label,status,now))
    cid = cur.lastrowid
    rows = c.execute("SELECT id,order_no,title,duration_sec,note FROM steps WHERE recipe_id=? ORDER BY order_no", (rid,)).fetchall()
    for i,(sid,oi,t,d,n) in enumerate(rows):
        st = "pending"; started=None; due=None; fin=None
        if i < done_upto: st="done"; fin=now-(done_upto-i)*60000
        elif i == running_idx: st="running"; started=now-100000; due=now+d*1000-100000
        c.execute("INSERT INTO cook_steps(cook_id,step_id,order_no,title,duration_sec,note,status,started_at,due_at,finished_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                  (cid,sid,oi,t,d,n,st,started,due,fin))
    return cid

make_cook(ids[0], "#1", "running", 1, 1)
make_cook(ids[2], "#2", "running", 0, 0)
make_cook(ids[1], "#1", "running", 2, 2)
c.commit()
print("rows:", c.execute("select count(*) from recipes").fetchone(), c.execute("select count(*) from cooks").fetchone(), c.execute("select count(*) from cook_steps").fetchone())
c.close()
