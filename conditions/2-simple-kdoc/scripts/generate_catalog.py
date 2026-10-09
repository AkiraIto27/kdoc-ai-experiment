#!/usr/bin/env python3
"""Generate and verify the deterministic, entirely fictional catalog fixture."""

import argparse
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import sys


SNAPSHOT_ID = "catalog-v1"
PRODUCT_COUNT = 500
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/catalog/products.json"
BASE_UPDATED_AT = datetime(2026, 9, 1, 0, 0, tzinfo=timezone.utc)
BASE_RESTOCK_AT = datetime(2026, 10, 10, 0, 0, tzinfo=timezone.utc)
COLORS = ("白砂", "青霧", "若草", "墨影", "麦色")
WAREHOUSES = (("warehouse-east", "東の保管庫"), ("warehouse-central", "中央の保管庫"), ("warehouse-west", "西の保管庫"))


@dataclass(frozen=True)
class Family:
    name: str
    purpose: str
    material: str
    feature: str
    care: str
    width: int
    depth: int
    height: int
    weight: int
    base_price: int
    unit_type: str
    pieces: int


@dataclass(frozen=True)
class Category:
    id: str
    name: str
    setting: str
    instruction: str
    families: tuple


CATEGORIES = (
    Category("storage", "収納", "机まわりや共用棚", "置き場所の奥行きを確かめ、出し入れする側に余白を残して使用してください。", (
        Family("卓上書類トレー", "作業中の書類を案件ごとにまとめる", "再生ポリプロピレン", "前側を低くした形で、重ねた紙の端をつまみやすくしています", "紙を取り出してから、乾いた柔らかい布で内側を拭いてください。", 250, 330, 55, 280, 680, "PIECE", 1),
        Family("取っ手付き保管ケース", "共用の小道具をひとまとめに持ち運ぶ", "ポリプロピレン", "上部の取っ手と平らな底面を組み合わせ、棚への戻しやすさを考えています", "取っ手の接続部に緩みがないか、使用前に確かめてください。", 220, 300, 180, 540, 1280, "PIECE", 1),
        Family("小物仕分けボックス", "細かな部品を種類別に分けて保管する", "ポリスチレン", "中身が寄りにくい浅い区画を備え、残量をひと目で確認しやすくしています", "細かな部品をすべて取り出してから、区画の隅を清掃してください。", 160, 210, 45, 190, 420, "PACK", 3),
        Family("積み重ね収納かご", "布類や軽量の備品を棚の中で整理する", "ポリプロピレン", "側面に指を掛けるくぼみを設け、棚の手前へ引き出しやすくしています", "積み重ねる前に底面の汚れを落とし、平らな棚面に置いてください。", 240, 340, 130, 380, 760, "PIECE", 1),
        Family("差し込み式棚仕切り", "棚の中で書類や薄い箱の境目を作る", "塗装スチール", "差し込み部分の角を丸く整え、並べる物の位置を調整しやすくしています", "棚板の厚みを確かめ、無理な力で差し込まないでください。", 80, 180, 190, 260, 510, "PACK", 2),
        Family("角形筆記具スタンド", "筆記具と短い定規を立てて整理する", "再生ポリプロピレン", "底に小さなくぼみを付け、細い筆記具が片側へ集まりにくい形にしています", "底にたまった消しごむのかすを除き、内側を布で拭いてください。", 80, 90, 105, 120, 390, "BOX", 6),
        Family("引き出し整理トレー", "引き出しの中の文具を用途別に並べる", "ポリプロピレン", "浅い縁と直線的な区画で、小さな文具を見失いにくくしています", "引き出しの可動範囲を確かめ、縁が引っ掛からない位置に置いてください。", 180, 250, 40, 170, 460, "PACK", 2),
        Family("配線収納ボックス", "机の下で余った配線をまとめて収める", "ABS樹脂", "両端に配線を通す切り欠きを設け、ふたを開けて確認しやすくしています", "使用する機器の放熱条件を確かめ、通気部分をふさがないでください。", 130, 280, 115, 430, 1380, "PIECE", 1),
        Family("仕分けカード立て", "保管棚に分類名や返却先を表示する", "PET樹脂", "カードを横から差し込める形で、表示内容を入れ替えやすくしています", "カードを抜いてから拭き、十分に乾かして差し戻してください。", 95, 45, 65, 45, 220, "BOX", 10),
        Family("机上二段ミニラック", "机上の小箱と日常備品を上下に分ける", "塗装スチール", "上段の奥に低い縁を設け、置いた小箱の位置をそろえやすくしています", "重い物は下段に置き、移動するときは中身を取り出してください。", 230, 310, 260, 1100, 2380, "PIECE", 1),
    )),
    Category("cleaning", "清掃", "共用机や備品の保管場所", "目立たない場所で素材との相性を確かめ、用途に合う道具を選んでください。", (
        Family("短柄デスクブラシ", "机上に残った細かなごみを集める", "ポリプロピレン・PET毛", "短い柄と横長の毛先で、机の奥から手前へ掃き寄せやすくしています", "毛先に付いたごみを取り除き、形を整えて乾燥させてください。", 65, 180, 45, 90, 430, "PACK", 2),
        Family("薄口ミニちり取り", "小さな作業台のごみをすくい取る", "ポリプロピレン", "先端を薄くした形で、集めた紙片を受け止めやすくしています", "縁を曲げたまま保管せず、洗った後は水分を拭き取ってください。", 170, 210, 55, 120, 380, "PIECE", 1),
        Family("繰り返し使う拭き取り布", "共用机の水滴や軽い汚れを拭き取る", "ポリエステル・ナイロン", "手のひらに収まりやすい厚みで、折り返して面を替えやすくしています", "使った後は汚れを洗い流し、十分に広げて乾かしてください。", 280, 280, 3, 35, 180, "BOX", 12),
        Family("角丸洗浄スポンジ", "洗える備品の表面をやさしくこする", "ポリウレタン", "握る位置を変えやすい角丸形状で、広い面と端を使い分けられます", "熱源から離し、使用後は絞って風通しのよい場所に置いてください。", 70, 110, 28, 18, 120, "PACK", 5),
        Family("交換用モップパッド", "平らな床のほこりをまとめて拭き取る", "ポリエステル", "端まで一定の厚みに仕上げ、取り付ける向きを確認しやすくしています", "取り付け部の寸法を確認し、洗った後は平らにして乾かしてください。", 110, 280, 12, 75, 390, "PACK", 3),
        Family("目盛り付き空スプレー容器", "清掃用の液体を区別して小分けにする", "ポリエチレン", "側面の記入欄と目盛りで、中身の種類と残量を確認しやすくしています", "中身の適合条件を確認し、違う液体を継ぎ足して混ぜないでください。", 80, 95, 210, 85, 320, "BOX", 6),
        Family("注ぎ口付き小型バケツ", "少量の水と清掃道具を持ち運ぶ", "ポリプロピレン", "片側に浅い注ぎ口を設け、後片付けで水を流す位置を合わせやすくしています", "取っ手と底面を確認し、使用後は内側の水分を残さず乾かしてください。", 220, 250, 210, 420, 860, "PIECE", 1),
        Family("画面用やわらかクロス", "表示機器の表面に付いたほこりを払う", "ポリエステル・ナイロン", "縁の厚みを抑えた柔らかな布で、折りたたんで収納しやすくしています", "機器側のお手入れ条件に従い、強く押し付けずに使ってください。", 180, 180, 2, 16, 150, "BOX", 10),
        Family("すき間掃除ブラシ", "棚の角や細いすき間のほこりを払う", "ポリプロピレン・ナイロン毛", "先端を細くまとめ、奥行きの浅いすき間へ向きを合わせやすくしています", "毛先を無理に押し込まず、付着したごみを取り除いて保管してください。", 20, 200, 25, 35, 260, "PACK", 4),
        Family("小型水切りワイパー", "洗える平面に残った水滴を集める", "ポリプロピレン・合成ゴム", "横長の先端と短い柄で、小さな面を少しずつ水切りしやすくしています", "ゴム部分の異物を落とし、先端を曲げずに保管してください。", 190, 160, 35, 110, 540, "PIECE", 1),
    )),
    Category("packing", "梱包", "発送準備台や資材棚", "包む物の寸法と重さを確かめ、必要に応じてほかの資材と組み合わせてください。", (
        Family("手切れ梱包テープ", "軽い箱のふたを留めて発送準備をする", "紙・粘着材", "巻き始めを見つけやすい印を添え、作業途中でも端を確保しやすくしています", "直射日光を避け、使用前に貼り付け面のほこりを取り除いてください。", 95, 95, 45, 180, 240, "BOX", 12),
        Family("折りたたみ緩衝シート", "小物の間に挟んで直接の接触を減らす", "発泡ポリエチレン", "折り目に沿って扱いやすい薄さで、小さな物の形に合わせて包めます", "尖った物で穴を開けないように扱い、乾いた場所で保管してください。", 250, 300, 2, 12, 70, "PACK", 20),
        Family("薄型発送封筒", "平らな書類や軽い付属品をまとめる", "クラフト紙", "折り返し部分を広めに取り、封をする位置をそろえやすくしています", "湿気を避けて平らに保管し、封入後に角の状態を確認してください。", 230, 320, 1, 30, 90, "BOX", 50),
        Family("角底持ち帰り袋", "小さな箱や備品をまとめて手渡す", "クラフト紙", "底を開くと自立しやすい形で、品物を順に入れやすくしています", "濡れた物を直接入れず、底の折り目を整えてから使用してください。", 180, 100, 260, 35, 80, "PACK", 20),
        Family("チャック付き仕分け袋", "小さな部品と付属品を分けて保管する", "ポリエチレン", "口元の段差で開く位置をつかみやすくし、中身の入れ替えを助けます", "尖った物を直接入れず、閉じる前に口元の異物を取り除いてください。", 140, 200, 1, 5, 30, "BOX", 100),
        Family("再利用結束バンド", "丸めた資材や配線を一時的にまとめる", "ナイロン", "留める位置を段階的に替えられる形で、繰り返しの仕分けに使えます", "締め付け過ぎを避け、留め具の傷や変形を確かめてください。", 12, 240, 3, 8, 65, "PACK", 10),
        Family("荷分け記入タグ", "荷物に分類名と行き先を書き添える", "厚紙・綿ひも", "書く面を広く取り、短いメモを見やすい位置に付けやすくしています", "水濡れを避け、ひもを掛ける部分の強さを確認してください。", 55, 100, 1, 4, 25, "BOX", 100),
        Family("差し込み箱内仕切り", "箱の中で小物同士の位置を分ける", "段ボール", "切り込みを組み合わせる形で、箱の中に区画を作りやすくしています", "折れや湿りがある物は使用せず、箱の内寸を確かめてください。", 200, 260, 90, 85, 160, "PACK", 10),
        Family("角当て保護パッド", "箱の中で品物の角と外箱の間を埋める", "発泡ポリエチレン", "直角に沿わせる形で、角を包む位置を合わせやすくしています", "物の角に合わせて置き、つぶれた部分がないか確認してください。", 65, 65, 65, 14, 75, "BOX", 40),
        Family("小巻まとめ用フィルム", "複数の軽い資材をひと束にまとめる", "ポリエチレン", "短い幅の巻き材で、持ち替えながら必要な位置に巻き付けやすくしています", "熱源を避け、巻く際に対象物を締め付け過ぎないでください。", 80, 80, 100, 230, 340, "PACK", 4),
    )),
    Category("office", "事務", "受付や共用の事務机", "使う場所の広さと扱う書類の量に合わせ、手が届きやすい位置へ置いてください。", (
        Family("差し替えメモホルダー", "作業中の短いメモを机上に立てる", "ABS樹脂・スチール", "低い台座と細い留め具で、手元の視界をふさぎにくい形にしています", "留め具を大きく広げ過ぎず、台座の汚れを布で拭いてください。", 55, 65, 95, 95, 360, "PACK", 3),
        Family("角丸記入ボード", "立ったまま受付票や点検票に記入する", "再生ポリプロピレン・スチール", "持つ側の角を丸く整え、用紙の端をそろえやすい平面にしています", "留め具に指を挟まないように扱い、表面を乾いた布で拭いてください。", 230, 320, 12, 240, 650, "PIECE", 1),
        Family("薄型書類ポケット", "一件分の書類と控えをまとめて持つ", "ポリプロピレン", "入口に小さな段差を付け、必要な紙を取り出しやすくしています", "紙を詰め込み過ぎず、折り目が付かないよう平らに保管してください。", 220, 310, 1, 20, 80, "BOX", 50),
        Family("見出し付き仕分け板", "書類の束を分類ごとに区切っておく", "厚紙", "見出し部分を少し広めに取り、分類名を読み取りやすくしています", "水分を避け、見出しを折り曲げない位置に収めてください。", 215, 305, 1, 18, 65, "PACK", 12),
        Family("卓上カードケース", "案内カードや名刺形の紙を取り置く", "PET樹脂", "前側のくぼみと浅い底で、カードの残り枚数を確認しやすくしています", "カードを取り出し、柔らかい布で内側のほこりを払ってください。", 100, 65, 45, 55, 290, "BOX", 8),
        Family("紙束用ワイドクリップ", "厚みのある資料を一時的にまとめる", "塗装スチール", "指を掛ける部分を長めに取り、付け外しする向きを確認しやすくしています", "挟める厚みを確かめ、金属部を無理に押し広げないでください。", 40, 50, 25, 22, 110, "PACK", 10),
        Family("丸角デスクマット", "机の記入面を区切って作業場所を作る", "熱可塑性エラストマー", "縁の段差を抑えた形で、紙を置き直す動きを妨げにくくしています", "机の表面との相性を確かめ、汚れは薄めた中性洗剤で拭いてください。", 400, 550, 2, 480, 1480, "PIECE", 1),
        Family("持ち運び印章トレー", "印章と小さな事務道具を一緒に運ぶ", "ポリプロピレン", "浅い持ち手と二つの区画で、使う物の置き場所をそろえやすくしています", "インクが付いたときは早めに拭き、平らな場所へ置いてください。", 120, 210, 55, 170, 590, "PIECE", 1),
        Family("貼り替え案内ラベル", "棚や引き出しに短い案内を表示する", "紙・粘着材", "文字をそろえる補助線を薄く入れ、手書きでも配置を決めやすくしています", "貼る面の相性を確かめ、はがすときは端からゆっくり引いてください。", 45, 90, 1, 2, 20, "BOX", 100),
        Family("折りたたみ資料スタンド", "参照資料を机上で起こして読みやすくする", "塗装スチール", "背面を折りたためる形で、使用後に棚のすき間へ戻しやすくしています", "開閉部に指を挟まないようにし、平らな面で使用してください。", 200, 170, 220, 430, 1680, "PIECE", 1),
    )),
    Category("safety", "安全", "共用通路や備品の置き場所", "設置場所の状況を確認し、通行や機器の操作を妨げない位置で使用してください。", (
        Family("足元注意プレート", "一時的に足元へ注意を向けてもらう", "ポリプロピレン", "持ち運びやすい薄い板に記入面を設け、状況に合う案内を添えられます", "表示が見える向きに置き、不要になったら速やかに片付けてください。", 210, 280, 5, 190, 620, "PIECE", 1),
        Family("区画表示ミニスタンド", "備品の仮置き範囲の目印を立てる", "ABS樹脂", "幅のある台座と差し替え面で、短い案内をその場に合わせて表示できます", "倒れやすい場所を避け、表示面と台座の状態を確認してください。", 130, 150, 220, 330, 980, "PIECE", 1),
        Family("色分け目印テープ", "棚の区画や保管位置を色で見分ける", "PET樹脂・粘着材", "一定幅の帯として貼れる形で、短い距離の目印を作りやすくしています", "貼る面の相性を確認し、浮きやはがれがあれば貼り替えてください。", 85, 85, 30, 120, 290, "PACK", 4),
        Family("丸角クッションパッド", "家具の角に触れやすい場所を覆う", "発泡ポリエチレン", "角に沿う切り込みを設け、貼り付ける位置を調整しやすくしています", "取り付け面を清掃し、ずれやはがれがないか定期的に確認してください。", 45, 45, 35, 10, 95, "BOX", 24),
        Family("開閉案内プレート", "扉や収納の使用状態をひと目で伝える", "PET樹脂", "表裏で表示を替えられる記入面を備え、その場の運用に合わせて使えます", "扉の動きを妨げない位置に掛け、取り付け部分を確認してください。", 90, 150, 2, 30, 260, "PACK", 5),
        Family("差し込み扉ストッパー", "作業中の扉の位置を一時的に保つ", "合成ゴム", "手でつかむ部分を広くした形で、抜き差しの位置を見つけやすくしています", "扉と床の条件を確認し、必要がなくなったら取り外してください。", 40, 120, 35, 110, 380, "PIECE", 1),
        Family("備品下敷きマット", "棚の上に置く軽い備品の位置を整える", "熱可塑性エラストマー", "表面に細かな凹凸を付け、小さな備品の置き場所を決めやすくしています", "接する素材との相性を確認し、汚れや水分を取り除いてください。", 180, 260, 3, 130, 470, "PACK", 3),
        Family("低段差配線カバー", "床に沿う短い配線の位置をまとめる", "軟質PVC", "中央の溝へ配線を収める形で、左右の端を床に沿わせやすくしています", "配線の太さと設置条件を確認し、浮いた部分があれば直してください。", 70, 500, 15, 350, 1180, "PIECE", 1),
        Family("携帯備品仕分けポーチ", "持ち出す小物の保管場所を決める", "ポリエステル", "内側の浅いポケットと大きな引き手で、中身を順に確認しやすくしています", "内容物は別途そろえ、汚れを拭き取ってよく乾かしてください。", 160, 230, 55, 120, 880, "PIECE", 1),
        Family("棚用識別プレート", "共用棚の区画と返却先を表示する", "ポリプロピレン", "大きな記入面と二つの取付穴で、棚の形に合わせて表示位置を選べます", "固定具は設置条件に合う物を使い、緩みがないか確認してください。", 100, 180, 3, 45, 210, "BOX", 10),
    )),
)


def instant(value):
    return value.isoformat(timespec="seconds").replace("+00:00", "Z")


def attribute(name, value):
    return {"name": name, "value": value}


def product_for(index, category, family, profile):
    size = profile // len(COLORS)
    color = COLORS[profile % len(COLORS)]
    size_label = "小型" if size == 0 else "中型"
    dimensions = {
        "width": family.width + family.width * size // 5,
        "depth": family.depth + family.depth * size // 5,
        "height": family.height + family.height * size // 5,
    }
    pieces = family.pieces
    minimum_order = 1 + index % 3
    unit_amount = family.base_price + 40 * size + 10 * (profile % len(COLORS))
    amount_yen = unit_amount * pieces
    state_index = (index - 1) % 20
    status = "AVAILABLE" if state_index < 14 else "OUT_OF_STOCK" if state_index < 17 else "UNKNOWN" if state_index < 19 else "DISCONTINUED"
    stock = 10 + index * 11 % 121 if status == "AVAILABLE" else None if status == "UNKNOWN" else 0
    restock = instant(BASE_RESTOCK_AT + timedelta(days=index % 12)) if status == "OUT_OF_STOCK" and index % 3 == 0 else None
    warehouse_quantities = [None, None, None] if stock is None else [stock // 2, stock // 3, stock - stock // 2 - stock // 3]
    warehouse_stocks = [
        {"warehouseId": warehouse_id, "name": name, "stockUnits": warehouse_quantities[warehouse_index], "leadTimeDays": (1, 2, 4)[warehouse_index] if status == "AVAILABLE" else None}
        for warehouse_index, (warehouse_id, name) in enumerate(WAREHOUSES)
    ]
    description = (
        f"{family.purpose}ための、{category.setting}で使う備品です。"
        f"{family.feature}。{size_label}の{color}色で、分類や置き場所に合わせて選べます。"
        f"{category.instruction}寸法と重量は備品一個分、表示価格は記載の内容量をまとめた一販売単位分です。"
    )
    care = [family.care, "保管前に汚れと水分を取り除き、直射日光と高温を避けてください。"]
    if index % 3 == 0:
        care.append("傷や変形が見つかった場合は使用を中止し、交換する品を確認してください。")
    variant_colors = (color, COLORS[(profile + 2) % len(COLORS)], COLORS[(profile + 3) % len(COLORS)])
    variants = [] if index % 37 == 0 else [
        {
            "id": f"product-{index:04d}-variant-{variant_index + 1}",
            "label": f"{variant_color}・{'標準仕上げ' if variant_index == 0 else '梨地仕上げ' if variant_index == 1 else '細目仕上げ'}",
            "additionalPriceYen": variant_index * 30 * pieces,
            "attributes": [attribute("色", variant_color), attribute("表面", ("標準", "梨地", "細目")[variant_index]), attribute("寸法区分", size_label)],
        }
        for variant_index, variant_color in enumerate(variant_colors[:2 + index % 2])
    ]
    tags = ("共用備品", "整理しやすい", "補充用")[:index % 4]
    return {
        "id": f"product-{index:04d}",
        "name": f"{family.name} {size_label}・{color}",
        "description": description,
        "category": {"id": category.id, "name": category.name},
        "price": {"amountYen": amount_yen, "taxIncluded": True},
        "salesUnit": {"type": family.unit_type, "piecesPerUnit": pieces, "minimumOrderUnits": minimum_order},
        "availability": {"status": status, "stockUnits": stock, "restockAt": restock},
        "specifications": {
            "weightGrams": family.weight + family.weight * size // 4,
            "dimensionsMm": dimensions,
            "attributes": [
                attribute("主な素材", family.material),
                attribute("色", color),
                attribute("寸法区分", f"{size_label}・寸法と重量は備品一個あたり（外装を除く）"),
                attribute("推奨する置き場所", category.setting),
                attribute("一販売単位の内容量", f"{pieces}個"),
                attribute("管理上の区分", f"{category.name}用品・共用備品（使い終わったら所定の場所へ返却）"),
            ],
            "careInstructions": care,
        },
        "variants": variants,
        "warehouseStocks": warehouse_stocks,
        "tags": list(tags),
        "replacementProductId": f"product-{index - 19:04d}" if status == "DISCONTINUED" and index % 40 == 0 else None,
        "updatedAt": instant(BASE_UPDATED_AT + timedelta(hours=(index - 1) // 4)),
    }


def generate_catalog():
    items = []
    for category in CATEGORIES:
        for family in category.families:
            for profile in range(10):
                items.append(product_for(len(items) + 1, category, family, profile))
    return {"snapshotId": SNAPSHOT_ID, "items": items, "pageInfo": {"nextCursor": None, "totalCount": len(items)}}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def exact_keys(value, keys, context):
    require(type(value) is dict and set(value) == set(keys.split()), f"{context}: unexpected JSON shape")


def positive_integer(value, context):
    require(type(value) is int and value > 0, f"{context}: expected positive integer")


def nonnegative_integer(value, context):
    require(type(value) is int and value >= 0, f"{context}: expected nonnegative integer")


def validate_instant(value, context):
    require(type(value) is str and value.endswith("Z"), f"{context}: expected UTC ISO instant")
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    require(instant(parsed) == value, f"{context}: noncanonical instant")
    return parsed


def validate_attributes(values, expected_count, context):
    require(type(values) is list and len(values) == expected_count, f"{context}: wrong attribute count")
    names = set()
    for value in values:
        exact_keys(value, "name value", context)
        require(all(type(value[key]) is str and value[key] for key in ("name", "value")), f"{context}: empty attribute")
        require(value["name"] not in names, f"{context}: duplicate attribute name")
        names.add(value["name"])


def validate_catalog(catalog):
    exact_keys(catalog, "snapshotId items pageInfo", "catalog")
    require(catalog["snapshotId"] == SNAPSHOT_ID, "catalog: incorrect snapshot ID")
    items = catalog["items"]
    require(type(items) is list and len(items) == PRODUCT_COUNT, "catalog: incorrect item count")
    exact_keys(catalog["pageInfo"], "nextCursor totalCount", "pageInfo")
    require(catalog["pageInfo"]["nextCursor"] is None, "pageInfo: full snapshot must have null nextCursor")
    require(type(catalog["pageInfo"]["totalCount"]) is int and catalog["pageInfo"]["totalCount"] == PRODUCT_COUNT, "pageInfo: incorrect total count")
    by_id = {item["id"]: item for item in items}
    require(len(by_id) == PRODUCT_COUNT, "catalog: duplicate product IDs")
    require(len({item["name"] for item in items}) == PRODUCT_COUNT, "catalog: duplicate product names")
    known_categories = {category.id: category.name for category in CATEGORIES}
    global_variant_ids = set()
    replacements = 0
    for index, item in enumerate(items, start=1):
        context = f"product-{index:04d}"
        exact_keys(item, "id name description category price salesUnit availability specifications variants warehouseStocks tags replacementProductId updatedAt", context)
        require(item["id"] == context, f"{context}: unstable product order or ID")
        require(type(item["name"]) is str and item["name"], f"{context}: empty name")
        require(type(item["description"]) is str and 100 <= len(item["description"]) <= 200, f"{context}: description outside 100–200 characters")
        category = item["category"]
        exact_keys(category, "id name", context)
        require(known_categories.get(category["id"]) == category["name"], f"{context}: unknown category")
        price = item["price"]
        exact_keys(price, "amountYen taxIncluded", context)
        positive_integer(price["amountYen"], context)
        require(type(price["taxIncluded"]) is bool, f"{context}: taxIncluded must be boolean")
        unit = item["salesUnit"]
        exact_keys(unit, "type piecesPerUnit minimumOrderUnits", context)
        require(unit["type"] in {"PIECE", "PACK", "BOX"}, f"{context}: invalid sales unit")
        positive_integer(unit["piecesPerUnit"], context)
        positive_integer(unit["minimumOrderUnits"], context)
        require(unit["piecesPerUnit"] == 1 if unit["type"] == "PIECE" else unit["piecesPerUnit"] > 1, f"{context}: inconsistent unit contents")
        family = CATEGORIES[(index - 1) // 100].families[((index - 1) % 100) // 10]
        profile = (index - 1) % 10
        expected_per_piece = family.base_price + 40 * (profile // len(COLORS)) + 10 * (profile % len(COLORS))
        require(price["amountYen"] == expected_per_piece * unit["piecesPerUnit"], f"{context}: amountYen must price one sales unit")
        availability = item["availability"]
        exact_keys(availability, "status stockUnits restockAt", context)
        status, stock, restock = (availability[key] for key in ("status", "stockUnits", "restockAt"))
        require(status in {"AVAILABLE", "OUT_OF_STOCK", "UNKNOWN", "DISCONTINUED"}, f"{context}: invalid availability")
        if status == "AVAILABLE":
            positive_integer(stock, context)
            require(stock >= unit["minimumOrderUnits"], f"{context}: available stock below minimum order")
        elif status == "UNKNOWN":
            require(stock is None, f"{context}: unknown stock must be null")
        else:
            require(type(stock) is int and stock == 0, f"{context}: unavailable stock must be zero")
        updated_at = validate_instant(item["updatedAt"], context)
        if restock is not None:
            require(status == "OUT_OF_STOCK", f"{context}: restock date requires known out-of-stock schedule")
            require(validate_instant(restock, context) > updated_at, f"{context}: restock precedes update")
        specifications = item["specifications"]
        exact_keys(specifications, "weightGrams dimensionsMm attributes careInstructions", context)
        positive_integer(specifications["weightGrams"], context)
        exact_keys(specifications["dimensionsMm"], "width depth height", context)
        for value in specifications["dimensionsMm"].values():
            positive_integer(value, context)
        validate_attributes(specifications["attributes"], 6, context)
        content_attribute = next(value["value"] for value in specifications["attributes"] if value["name"] == "一販売単位の内容量")
        require(content_attribute == f"{unit['piecesPerUnit']}個", f"{context}: content attribute differs from sales unit")
        care = specifications["careInstructions"]
        require(type(care) is list and 2 <= len(care) <= 3 and all(type(line) is str and line for line in care), f"{context}: invalid care instructions")
        variants = item["variants"]
        require(type(variants) is list and len(variants) in {0, 2, 3}, f"{context}: invalid variant count")
        for variant in variants:
            exact_keys(variant, "id label additionalPriceYen attributes", context)
            require(type(variant["id"]) is str and variant["id"].startswith(context + "-variant-") and variant["id"] not in global_variant_ids, f"{context}: invalid or duplicate variant ID")
            global_variant_ids.add(variant["id"])
            require(type(variant["label"]) is str and variant["label"], f"{context}: empty variant label")
            nonnegative_integer(variant["additionalPriceYen"], context)
            validate_attributes(variant["attributes"], 3, context)
        warehouses = item["warehouseStocks"]
        require(type(warehouses) is list and len(warehouses) == len(WAREHOUSES), f"{context}: wrong warehouse count")
        require(len({warehouse["warehouseId"] for warehouse in warehouses}) == len(WAREHOUSES), f"{context}: duplicate warehouse IDs")
        for warehouse in warehouses:
            exact_keys(warehouse, "warehouseId name stockUnits leadTimeDays", context)
            require((warehouse["warehouseId"], warehouse["name"]) in WAREHOUSES, f"{context}: unknown warehouse")
            if stock is None:
                require(warehouse["stockUnits"] is None and warehouse["leadTimeDays"] is None, f"{context}: unknown warehouse values must be null")
            else:
                nonnegative_integer(warehouse["stockUnits"], context)
            if warehouse["leadTimeDays"] is not None:
                nonnegative_integer(warehouse["leadTimeDays"], context)
        if stock is not None:
            require(sum(warehouse["stockUnits"] for warehouse in warehouses) == stock, f"{context}: warehouse stocks do not sum to total sales units")
        tags = item["tags"]
        require(type(tags) is list and 0 <= len(tags) <= 3 and all(type(tag) is str and tag for tag in tags) and len(set(tags)) == len(tags), f"{context}: invalid tags")
        replacement = item["replacementProductId"]
        if replacement is not None:
            require(type(replacement) is str and status == "DISCONTINUED" and replacement != context and replacement in by_id, f"{context}: invalid replacement reference")
            require(by_id[replacement]["availability"]["status"] == "AVAILABLE", f"{context}: replacement must be available")
            replacements += 1
    require(Counter(item["category"]["id"] for item in items) == Counter({category.id: 100 for category in CATEGORIES}), "catalog: category counts differ")
    require(Counter(item["availability"]["status"] for item in items) == Counter(AVAILABLE=350, OUT_OF_STOCK=75, UNKNOWN=50, DISCONTINUED=25), "catalog: availability coverage differs")
    require(0 < replacements < 25, "catalog: discontinued items must include both null and valid replacements")
    require(len({item["updatedAt"] for item in items}) < len(items), "catalog: missing tied update times")
    require(any(not item["variants"] for item in items) and any(not item["tags"] for item in items), "catalog: missing empty-array examples")
    require(any(item["availability"]["restockAt"] for item in items), "catalog: missing known restock examples")


def encode_catalog(catalog):
    return (json.dumps(catalog, ensure_ascii=False, separators=(",", ":"), allow_nan=False) + "\n").encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Validate generated data and require the existing fixture to match byte for byte.")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="Fixture path; defaults to app/src/main/assets/catalog/products.json.")
    args = parser.parse_args()
    try:
        catalog = generate_catalog()
        validate_catalog(catalog)
        encoded = encode_catalog(catalog)
        require(encode_catalog(generate_catalog()) == encoded, "catalog: generation is not deterministic")
        require(encode_catalog(json.loads(encoded)) == encoded, "catalog: JSON round trip differs")
        if args.check:
            require(args.output.is_file(), "fixture missing; run without --check to generate it")
            require(args.output.read_bytes() == encoded, "fixture differs from deterministic generation")
            state = "checked"
        else:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_bytes(encoded)
            require(args.output.read_bytes() == encoded, "fixture write verification failed")
            state = "generated"
        print(json.dumps({"status": state, "items": len(catalog["items"]), "bytes": len(encoded), "sha256": hashlib.sha256(encoded).hexdigest(), "availabilityCounts": dict(sorted(Counter(item["availability"]["status"] for item in catalog["items"]).items()))}, separators=(",", ":")))
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"catalog error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
