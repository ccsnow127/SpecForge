"""
Discounted UCB bandit for entity selection.
Chooses which entity to refine at each search step based on discounted observed rewards.
"""

from math import sqrt, log, inf


class DiscountedUCB:
    def __init__(self, entities: list[str], gamma: float = 0.95, C: float = 2 * sqrt(2)):
        self.gamma = gamma
        self.C = C
        self.stats: dict[str, dict[str, float]] = {
            e: {"n_gamma": 0.0, "reward_gamma": 0.0} for e in entities
        }
        self.N_gamma: float = 0.0

    def select(self, eligible: list[str], entity_phi: dict[str, float]) -> str:
        best_entity = eligible[0]
        best_score = -inf

        for e in eligible:
            gap_e = 1.0 - entity_phi.get(e, 0.0)
            s = self.stats.get(e)
            if s is None or s["n_gamma"] < 1e-8:
                score = inf * gap_e if gap_e > 1e-8 else 0.0
                if score > best_score:
                    best_score = score
                    best_entity = e
                continue
            mean = s["reward_gamma"] / s["n_gamma"]
            exploration = self.C * sqrt(log(max(self.N_gamma, 1.0)) / s["n_gamma"])
            score = (mean + exploration) * gap_e
            if score > best_score:
                best_score = score
                best_entity = e

        return best_entity

    def get_scores(self, eligible: list[str], entity_phi: dict[str, float]) -> dict[str, dict]:
        scores = {}
        for e in eligible:
            gap_e = 1.0 - entity_phi.get(e, 0.0)
            s = self.stats.get(e)
            if s is None or s["n_gamma"] < 1e-8:
                scores[e] = {
                    "score": float('inf') if gap_e > 1e-8 else 0.0,
                    "mean": 0.0,
                    "exploration": float('inf'),
                    "gap": round(gap_e, 4),
                }
            else:
                mean = s["reward_gamma"] / s["n_gamma"]
                exploration = self.C * sqrt(log(max(self.N_gamma, 1.0)) / s["n_gamma"])
                score = (mean + exploration) * gap_e
                scores[e] = {
                    "score": round(score, 4),
                    "mean": round(mean, 4),
                    "exploration": round(exploration, 4),
                    "gap": round(gap_e, 4),
                }
        return scores

    def update(self, entity: str, raw_reward: float, gap: float):
        for s in self.stats.values():
            s["n_gamma"] *= self.gamma
            s["reward_gamma"] *= self.gamma
        self.N_gamma *= self.gamma

        normalized_reward = raw_reward / gap if gap > 1e-6 else 0.0

        if entity in self.stats:
            self.stats[entity]["n_gamma"] += 1.0
            self.stats[entity]["reward_gamma"] += normalized_reward
        self.N_gamma += 1.0
