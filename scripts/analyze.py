"""
Đọc results/results.csv -> bảng thống kê + đồ thị + gợi ý CUTOFF tốt nhất.

Cài thư viện (một lần):  pip install pandas matplotlib
Chạy từ thư mục gốc project:  python scripts/analyze.py
Đầu ra:
  results/summary.csv      bảng thống kê đầy đủ (n, type, cutoff, mean, std, median, ...)
  results/summary.md       bảng best cutoff, copy thẳng vào báo cáo
  results/figures/*.png    đồ thị
"""
import os
import sys

import matplotlib
matplotlib.use("Agg")  # không cần cửa sổ GUI
import matplotlib.pyplot as plt
import pandas as pd

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CSV = os.path.join(ROOT, "results", "results.csv")
FIG_DIR = os.path.join(ROOT, "results", "figures")
TEXTBOOK_CUTOFFS = (7, 10)  # giá trị đề cập

if not os.path.exists(CSV):
    sys.exit("Không thấy results/results.csv. Hãy chạy Experiment trước.")
os.makedirs(FIG_DIR, exist_ok=True)

df = pd.read_csv(CSV)

# ---------- 1. Thống kê theo (n, type, cutoff) ----------
g = df.groupby(["n", "type", "cutoff"])["time_ms"]
summary = g.agg(trials="count", mean="mean", std="std", median="median",
                min="min", max="max").reset_index()
summary["sem"] = summary["std"] / summary["trials"] ** 0.5
summary["ci95"] = 1.96 * summary["sem"]  # xấp xỉ khoảng tin cậy 95% của mean

# thời gian so với cutoff = 1 (mergesort thuần) cùng (n, type)
base = summary[summary["cutoff"] == 1].set_index(["n", "type"])["mean"]
summary["speedup_vs_c1"] = summary.apply(
    lambda r: base[(r["n"], r["type"])] / r["mean"], axis=1)

summary.round(4).to_csv(os.path.join(ROOT, "results", "summary.csv"), index=False)

# ---------- 2. Cutoff tốt nhất cho từng (n, type) ----------
rows = []
for (n, typ), s in summary.groupby(["n", "type"]):
    best = s.loc[s["mean"].idxmin()]
    best_med = s.loc[s["median"].idxmin()]
    # cutoff "không khác biệt đáng kể": mean nằm trong best.mean + (ci95 của best + ci95 của chính nó)
    close = s[s["mean"] <= best["mean"] + best["ci95"] + s["ci95"]]["cutoff"].tolist()
    row = {
        "n": n, "type": typ,
        "best_by_mean": int(best["cutoff"]),
        "best_mean_ms": round(best["mean"], 3),
        "best_by_median": int(best_med["cutoff"]),
        "equivalent_cutoffs": ",".join(str(int(c)) for c in close),
        "speedup_vs_c1": round(best["speedup_vs_c1"], 3),
    }
    for tc in TEXTBOOK_CUTOFFS:
        r = s[s["cutoff"] == tc]
        row[f"mean_ms_c{tc}"] = round(r["mean"].iloc[0], 3) if len(r) else None
    rows.append(row)
best_df = pd.DataFrame(rows)

# Tổng hợp: cutoff nào thường xuyên nằm trong nhóm tốt nhất
print("\n===== CUTOFF TỐT NHẤT THEO TỪNG CẤU HÌNH =====")
print(best_df.to_string(index=False))

# Xếp hạng trung bình trên mọi cấu hình (chuẩn hoá theo thời gian tốt nhất của từng cấu hình)
summary["rel_to_best"] = summary.groupby(["n", "type"])["mean"].transform(lambda x: x / x.min())
overall = summary.groupby("cutoff")["rel_to_best"].mean().sort_values()
print("\n===== TRUNG BÌNH CHUẨN HOÁ TRÊN TẤT CẢ CẤU HÌNH (1.000 = tốt nhất) =====")
print(overall.round(4).head(8).to_string())

with open(os.path.join(ROOT, "results", "summary.md"), "w", encoding="utf-8") as f:
    f.write("## Cutoff tốt nhất theo từng cấu hình\n\n")
    try:
        f.write(best_df.to_markdown(index=False))  # cần: pip install tabulate
    except ImportError:
        f.write(best_df.to_string(index=False))
    f.write("\n\n## Xếp hạng chuẩn hoá trên mọi cấu hình\n\n")
    f.write(overall.round(4).to_string())
    f.write("\n")

# ---------- 3. Đồ thị ----------
types = sorted(summary["type"].unique())
sizes = sorted(summary["n"].unique())

# 3a. Với mỗi loại dữ liệu: thời gian (ms) theo cutoff, mỗi N một ô, có thanh lỗi = std
for typ in types:
    fig, axes = plt.subplots(1, len(sizes), figsize=(5 * len(sizes), 4), squeeze=False)
    for ax, n in zip(axes[0], sizes):
        s = summary[(summary["type"] == typ) & (summary["n"] == n)].sort_values("cutoff")
        ax.errorbar(s["cutoff"], s["mean"], yerr=s["std"], marker="o", capsize=3)
        b = s.loc[s["mean"].idxmin()]
        ax.axvline(b["cutoff"], color="red", linestyle="--", alpha=0.6,
                   label=f"best = {int(b['cutoff'])}")
        ax.set_title(f"{typ}, N = {n:,}")
        ax.set_xlabel("CUTOFF")
        ax.set_ylabel("Thời gian (ms)")
        ax.grid(alpha=0.3)
        ax.legend()
    fig.tight_layout()
    fig.savefig(os.path.join(FIG_DIR, f"time_vs_cutoff_{typ}.png"), dpi=150)
    plt.close(fig)

# 3b. So sánh các N trên cùng một đồ thị: thời gian chuẩn hoá theo cutoff tốt nhất
for typ in types:
    fig, ax = plt.subplots(figsize=(7, 4.5))
    for n in sizes:
        s = summary[(summary["type"] == typ) & (summary["n"] == n)].sort_values("cutoff")
        ax.plot(s["cutoff"], s["mean"] / s["mean"].min(), marker="o", label=f"N = {n:,}")
    ax.set_title(f"Thời gian chuẩn hoá (1.0 = tốt nhất) - {typ}")
    ax.set_xlabel("CUTOFF")
    ax.set_ylabel("mean / min(mean)")
    ax.grid(alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(os.path.join(FIG_DIR, f"normalized_{typ}.png"), dpi=150)
    plt.close(fig)

# 3c. Boxplot phân bố các lần đo cho một (n, type) lớn nhất, random
n_big = max(sizes)
for typ in types:
    d = df[(df["n"] == n_big) & (df["type"] == typ)]
    cs = sorted(d["cutoff"].unique())
    fig, ax = plt.subplots(figsize=(9, 4.5))
    ax.boxplot([d[d["cutoff"] == c]["time_ms"] for c in cs], showfliers=True)
    ax.set_xticklabels(cs)
    ax.set_title(f"Phân bố thời gian các lần đo - {typ}, N = {n_big:,}")
    ax.set_xlabel("CUTOFF")
    ax.set_ylabel("Thời gian (ms)")
    ax.grid(alpha=0.3)
    fig.tight_layout()
    fig.savefig(os.path.join(FIG_DIR, f"boxplot_{typ}_{n_big}.png"), dpi=150)
    plt.close(fig)

print(f"\nĐã ghi: results/summary.csv, results/summary.md, {FIG_DIR}")