# -*- coding: utf-8 -*-
"""生成两份演示数据库：中文内容与英文内容，仅用于截图，不进仓库。"""
import sqlite3, time, shutil, os

BASE = os.path.join(os.path.dirname(__file__), "..", "shots")
BASE = os.path.abspath(BASE)

ZH = {
    1: ("红烧肉", "经典本帮，肥而不腻", [
        ("五花肉切块焯水", "冷水下锅，加姜片料酒"),
        ("炒糖色", "小火慢炒，别糊了"),
        ("加水炖煮", "没过肉面两指"),
        ("大火收汁", "翻动防粘底"),
    ]),
    2: ("番茄牛腩", "酸甜浓郁，拌饭一绝", [
        ("牛腩焯水", ""),
        ("番茄炒出沙", "加少许糖"),
        ("炖煮入味", "小火"),
        ("收汁调味", ""),
    ]),
    3: ("银耳羹", "小火慢炖出胶", [
        ("泡发银耳", "提前两小时冷水泡"),
        ("小火慢炖", "加冰糖"),
        ("加枸杞", ""),
    ]),
}

EN = {
    1: ("Braised pork belly", "Classic Shanghainese, rich but not greasy", [
        ("Blanch pork belly", "cold water, ginger and rice wine"),
        ("Caramelise sugar", "low heat, do not burn"),
        ("Braise with water", "two fingers above the meat"),
        ("Reduce the sauce", "stir so it does not stick"),
    ]),
    2: ("Tomato beef stew", "Tangy and rich, perfect over rice", [
        ("Blanch the beef", ""),
        ("Fry tomato into paste", "a pinch of sugar"),
        ("Simmer to infuse", "low heat"),
        ("Reduce and season", ""),
    ]),
    3: ("Tremella soup", "Slow simmered until silky", [
        ("Soak the tremella", "two hours in cold water first"),
        ("Simmer gently", "add rock sugar"),
        ("Add goji berries", ""),
    ]),
}


def build(src, dst, data, first_note):
    shutil.copyfile(src, dst)
    c = sqlite3.connect(dst)
    now = int(time.time() * 1000)
    for rid, (name, note, rows) in data.items():
        c.execute("UPDATE recipes SET name=?, note=? WHERE id=?", (name, note, rid))
        for i, (title, snote) in enumerate(rows):
            c.execute("UPDATE steps SET title=?, note=? WHERE recipe_id=? AND order_no=?", (title, snote, rid, i))
            c.execute(
                "UPDATE cook_steps SET title=?, note=? WHERE cook_id IN (SELECT id FROM cooks WHERE recipe_id=?) AND order_no=?",
                (title, snote, rid, i),
            )
    c.execute("UPDATE cook_steps SET note=? WHERE cook_id=1 AND order_no=0", (first_note,))
    c.execute("UPDATE cook_steps SET status='pending', started_at=NULL, due_at=NULL, finished_at=NULL")
    c.execute("UPDATE cooks SET status='running', finished_at=NULL")
    c.execute("UPDATE cook_steps SET status='done', finished_at=? WHERE cook_id=1 AND order_no=0", (now - 300000,))
    c.execute("UPDATE cook_steps SET status='running', started_at=?, due_at=? WHERE cook_id=1 AND order_no=1", (now - 100000, now + 480000))
    c.execute("UPDATE cook_steps SET status='running', started_at=?, due_at=? WHERE cook_id=2 AND order_no=0", (now - 60000, now + 1140000))
    c.execute("UPDATE cook_steps SET status='running', started_at=?, due_at=? WHERE cook_id=3 AND order_no=2", (now - 30000, now + 150000))
    c.commit()
    c.close()
    print("wrote", dst)


if __name__ == "__main__":
    src = os.path.join(BASE, "cur.db")
    build(src, os.path.join(BASE, "db_zh.db"), ZH, "这次火大了，下次小一点")
    build(src, os.path.join(BASE, "db_en.db"), EN, "Heat was too high, lower it next time")
