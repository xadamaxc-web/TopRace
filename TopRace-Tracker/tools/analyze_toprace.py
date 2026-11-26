#!/usr/bin/env python3
"""TOP RACE analysis. Usage: python analyze_toprace.py toprace_rounds_*.csv

Answers, with p-values and plain verdicts: do any cars win more/less than their odds imply, does
any version (MATH/BASE/DATA/BLEND/RECENT) beat chance, do winners repeat more than chance, are the
confidence % honest, and (S8+ rounds) where the winner sat in each version's full ordering. Needs: pandas, scipy, numpy.
"""
import sys
import numpy as np
import pandas as pd
from scipy import stats

ODDS = {"Shark": 2.8, "Eagle": 3.2, "Jeep": 3.8, "Thunder": 3.8, "Ape": 5.5, "Dragon": 6.8,
        "Flash": 6.9, "Power": 11.7, "Whale": 14.9}
FAST = {"Thunder", "Flash", "Eagle", "Power", "Whale"}
FAST_ROADS = {"Desert", "Expressway", "Highway"}
VERSIONS = ["math", "base", "data", "blend", "recent", "live", "mine"]
ALPHA = 0.05


def verdict(p, n_tests=1):
    if p is None or np.isnan(p):
        return "n/a"
    if p < ALPHA / n_tests:
        return "LOOKS REAL"
    if p < ALPHA:
        return "maybe (could be luck, many tests run)"
    return "consistent with luck"


def offered(row):
    return [row[f"car{i}"] for i in range(1, 6)]


def implied(row):
    """P(each offered car wins) if wins follow base odds, normalised over the 5 offered."""
    cars = offered(row)
    w = np.array([1.0 / ODDS[c] for c in cars])
    return dict(zip(cars, w / w.sum()))


def section(t):
    print("\n" + "=" * 70 + f"\n{t}\n" + "=" * 70)


def main(path):
    df = pd.read_csv(path)
    df = df[df["winner"].notna()].reset_index(drop=True)
    n = len(df)
    section("OVERVIEW")
    print(f"Rounds with a winner: {n}")
    if n < 100:
        print("WARNING: under 100 rounds. Everything below is noise-level; use it to check the data, not to decide.")
    elif n < 300:
        print("Note: under 300 rounds. Only large effects can show up.")

    # 1. cars vs odds
    section("1. DO CARS WIN MORE OR LESS THAN THEIR ODDS IMPLY?")
    rows = []
    probs = [implied(r) for _, r in df.iterrows()]
    for car in ODDS:
        exp = var = obs = off = 0.0
        for (_, r), pr in zip(df.iterrows(), probs):
            if car in pr:
                off += 1
                p = pr[car]
                exp += p
                var += p * (1 - p)
                obs += 1 if r["winner"] == car else 0
        if off == 0:
            continue
        z = (obs - exp) / np.sqrt(var) if var > 0 else 0.0
        pv = 2 * (1 - stats.norm.cdf(abs(z)))
        rows.append((car, int(off), int(obs), exp, z, pv))
    print(f"{'car':8}{'offered':>8}{'wins':>6}{'expected':>10}{'z':>7}{'p':>8}  verdict")
    for car, off, obs, exp, z, pv in sorted(rows, key=lambda x: x[5]):
        print(f"{car:8}{off:8d}{obs:6d}{exp:10.1f}{z:7.2f}{pv:8.3f}  {verdict(pv, len(rows))}")

    # fast-tier winner share by road type
    print("\nFast-tier cars winning, by revealed road type (expected from odds):")
    for label, mask in (("fast roads", df["revealed_road"].isin(FAST_ROADS)), ("slow roads", ~df["revealed_road"].isin(FAST_ROADS))):
        sub = df[mask]
        if len(sub) < 10:
            print(f"  {label}: only {len(sub)} rounds")
            continue
        exp = sum(sum(p for c, p in implied(r).items() if c in FAST) for _, r in sub.iterrows())
        var = sum(sum(p for c, p in implied(r).items() if c in FAST) *
                  (1 - sum(p for c, p in implied(r).items() if c in FAST)) for _, r in sub.iterrows())
        obs = int(sub["winner"].isin(FAST).sum())
        z = (obs - exp) / np.sqrt(var) if var > 0 else 0
        pv = 2 * (1 - stats.norm.cdf(abs(z)))
        print(f"  {label}: {obs} of {len(sub)} (expected {exp:.1f})  p={pv:.3f}  {verdict(pv, 2)}")

    # 2. versions
    section("2. DOES ANY VERSION BEAT CHANCE?  (chance: W1 20%, W1-W3 60%, mean winner rank 3.0)")
    have = [v for v in VERSIONS if f"rank_{v}" in df and df[f"rank_{v}"].notna().sum() > 0]
    if not have:
        print("No rank columns found (rounds from before S7 have none).")
    print(f"{'version':8}{'n':>5}{'W1%':>7}{'W2%':>7}{'W3%':>7}{'LOSE%':>7}{'avg rank':>10}{'p(W1)':>8}{'p(top3)':>9}{'p(rank)':>9}")
    for v in have:
        r = df[f"rank_{v}"].dropna().astype(int)
        m = len(r)
        w1, w2, w3 = (r == 1).sum(), (r == 2).sum(), (r == 3).sum()
        lose = (r >= 4).sum()
        p1 = stats.binomtest(int(w1), m, 0.2).pvalue
        p3 = stats.binomtest(int(w1 + w2 + w3), m, 0.6).pvalue
        z = (r.mean() - 3.0) / np.sqrt(2.0 / m)
        pr = 2 * (1 - stats.norm.cdf(abs(z)))
        print(f"{v:8}{m:5d}{100*w1/m:7.1f}{100*w2/m:7.1f}{100*w3/m:7.1f}{100*lose/m:7.1f}{r.mean():10.2f}{p1:8.3f}{p3:9.3f}{pr:9.3f}")
    if have:
        print("\nLower average rank is better. A version is only worth trusting if its p-values are small")
        print(f"AND its numbers are on the good side of chance. With {len(have)} versions tested, use p < {ALPHA/len(have):.3f}.")
        base = df["rank_base"].dropna().astype(int) if "rank_base" in df else None
        if base is not None and len(base):
            print("BASE = just pick the shortest odds. Any version that cannot beat BASE adds nothing.")

    # 3. repeats
    section("3. DO WINNERS REPEAT MORE THAN CHANCE?")
    if n >= 20:
        exp = var = obs = 0.0
        for i in range(1, n):
            prev = df.loc[i - 1, "winner"]
            pr = probs[i]
            p = pr.get(prev, 0.0)
            exp += p
            var += p * (1 - p)
            obs += 1 if df.loc[i, "winner"] == prev else 0
        z = (obs - exp) / np.sqrt(var) if var > 0 else 0
        pv = 2 * (1 - stats.norm.cdf(abs(z)))
        print(f"Same car won twice in a row: {int(obs)} times (expected {exp:.1f})  p={pv:.3f}  {verdict(pv)}")
        seq = df["winner"].isin(FAST).astype(int).values
        runs = 1 + (seq[1:] != seq[:-1]).sum()
        n1, n0 = seq.sum(), len(seq) - seq.sum()
        if n1 and n0:
            mu = 2 * n1 * n0 / (n1 + n0) + 1
            sd = np.sqrt((mu - 1) * (mu - 2) / (n1 + n0 - 1))
            z = (runs - mu) / sd if sd > 0 else 0
            pv = 2 * (1 - stats.norm.cdf(abs(z)))
            print(f"Fast-tier/Medium-tier winner streakiness: {runs} runs (expected {mu:.1f})  p={pv:.3f}  {verdict(pv)}")
    else:
        print("Need 20+ rounds.")

    # 4. calibration
    section("4. IS THE CONFIDENCE % HONEST?  (pick #1 of BLEND)")
    if "conf_shown" in df and "rank_blend" in df:
        c = df[df["conf_shown"].notna() & df["rank_blend"].notna()].copy()
        if len(c):
            c["conf1"] = c["conf_shown"].astype(str).str.split("|").str[0].astype(float)
            c["hit"] = (c["rank_blend"] == 1).astype(int)
            bins = [0, 22, 26, 30, 35, 101]
            c["bin"] = pd.cut(c["conf1"], bins=bins, right=False)
            print(f"{'confidence bin':16}{'rounds':>7}{'avg conf%':>11}{'actual hit%':>13}")
            for b, g in c.groupby("bin", observed=True):
                print(f"{str(b):16}{len(g):7d}{g['conf1'].mean():11.1f}{100*g['hit'].mean():13.1f}")
            print("If avg conf and actual hit differ a lot, the % is not trustworthy yet.")
        else:
            print("No rounds with confidence data yet.")
    # 5. full orderings (S8+)
    section("5. FULL ORDERINGS OF THE 5 CARS  (rounds saved from S8 on)")
    ORDERED = [v for v in ("math", "data", "blend", "recent") if f"order_{v}" in df]
    o = df[df[f"order_{ORDERED[0]}"].notna()].reset_index(drop=True) if ORDERED else df.iloc[0:0]
    if len(o) == 0:
        print("No rounds with saved orderings yet (rounds from before S8 have none).")
    else:
        print(f"Rounds with orderings: {len(o)}")
        order = {v: o[f"order_{v}"].astype(str).str.split("|") for v in ORDERED}
        # BASE is fixed (shortest odds first), so it is rebuilt here instead of saved
        order["base"] = o.apply(lambda r: sorted(offered(r), key=lambda c: (ODDS[c], c)), axis=1)

        bad = 0
        for v, seqs in order.items():
            col = f"rank_{v}"
            if col not in o:
                continue
            for seq, w, rk in zip(seqs, o["winner"], o[col]):
                if pd.isna(rk) or w not in seq or seq.index(w) + 1 != int(rk):
                    bad += 1
        print("Integrity check (saved order vs saved winner rank): " + ("OK" if bad == 0 else f"{bad} MISMATCHES, check the export"))

        print("\nWhere the winner sat in each version's ordering (% of rounds, chance = 20% each):")
        print(f"{'version':8}{'1st':>7}{'2nd':>7}{'3rd':>7}{'4th':>7}{'5th':>7}")
        for v, seqs in order.items():
            pos = [seq.index(w) + 1 for seq, w in zip(seqs, o["winner"]) if w in seq]
            if pos:
                print(f"{v:8}" + "".join(f"{100 * pos.count(k) / len(pos):7.1f}" for k in range(1, 6)))

        print("\nHow often two versions put the SAME car first (versions that always agree add nothing):")
        names = list(order)
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                same = np.mean([a[0] == b[0] for a, b in zip(order[names[i]], order[names[j]])])
                print(f"  {names[i]:6} vs {names[j]:6}: {100 * same:5.1f}%")

    print("\nDone. Verdict words: LOOKS REAL = unlikely to be luck; consistent with luck = no evidence of an effect.")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
