"""Tests for pso_simple.py"""

import pytest
from unittest.mock import patch
from pso_simple import Particle, minimize


# Simple cost function for testing
def sphere(x):
    """Sphere function: sum of squares, minimum at origin."""
    return sum(xi ** 2 for xi in x)


# === Particle.__init__ tests ===

def test_Particle___init___position():
    import pso_simple
    pso_simple.num_dimensions = 3
    p = Particle([1.0, 2.0, 3.0])
    assert p.position_i == [1.0, 2.0, 3.0]

def test_Particle___init___velocity_length():
    import pso_simple
    pso_simple.num_dimensions = 3
    p = Particle([1.0, 2.0, 3.0])
    assert len(p.velocity_i) == 3

def test_Particle___init___defaults():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([0.0, 0.0])
    assert p.pos_best_i == []
    assert p.err_best_i == -1
    assert p.err_i == -1

def test_Particle___init___velocity_range():
    import pso_simple
    pso_simple.num_dimensions = 100
    p = Particle([0.0] * 100)
    for v in p.velocity_i:
        assert -1 <= v <= 1


# === Particle.evaluate tests ===

def test_Particle_evaluate_updates_error():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([3.0, 4.0])
    p.evaluate(sphere)
    assert p.err_i == 25.0

def test_Particle_evaluate_updates_best_on_first_call():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([3.0, 4.0])
    p.evaluate(sphere)
    assert p.err_best_i == 25.0
    assert p.pos_best_i == [3.0, 4.0]

def test_Particle_evaluate_updates_best_on_improvement():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([3.0, 4.0])
    p.evaluate(sphere)
    # Move to a better position manually
    p.position_i = [1.0, 1.0]
    p.evaluate(sphere)
    assert p.err_best_i == 2.0
    assert p.pos_best_i == [1.0, 1.0]

def test_Particle_evaluate_no_update_on_worse():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([1.0, 1.0])
    p.evaluate(sphere)
    assert p.err_best_i == 2.0
    p.position_i = [5.0, 5.0]
    p.evaluate(sphere)
    assert p.err_best_i == 2.0
    assert p.pos_best_i == [1.0, 1.0]


# === Particle.update_velocity tests ===

def test_Particle_update_velocity_changes_velocity():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([5.0, 5.0])
    p.pos_best_i = [5.0, 5.0]
    p.err_best_i = 50.0
    old_velocity = p.velocity_i.copy()
    p.update_velocity([0.0, 0.0])
    assert p.velocity_i != old_velocity

def test_Particle_update_velocity_length_preserved():
    import pso_simple
    pso_simple.num_dimensions = 3
    p = Particle([1.0, 2.0, 3.0])
    p.pos_best_i = [1.0, 2.0, 3.0]
    p.err_best_i = 14.0
    p.update_velocity([0.0, 0.0, 0.0])
    assert len(p.velocity_i) == 3

def test_Particle_update_velocity_at_optimum():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([0.0, 0.0])
    p.pos_best_i = [0.0, 0.0]
    p.err_best_i = 0.0
    # When particle is at best position and global best, cognitive and social terms are 0
    p.update_velocity([0.0, 0.0])
    # Only inertia component remains: w * old_velocity
    for i in range(2):
        assert abs(p.velocity_i[i]) <= 1.0  # bounded by initial velocity * w


# === Particle.update_position tests ===

def test_Particle_update_position_basic():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([0.0, 0.0])
    p.velocity_i = [1.0, -1.0]
    bounds = [(-10, 10), (-10, 10)]
    p.update_position(bounds)
    assert p.position_i == [1.0, -1.0]

def test_Particle_update_position_clamp_upper():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([9.0, 0.0])
    p.velocity_i = [5.0, 0.0]
    bounds = [(-10, 10), (-10, 10)]
    p.update_position(bounds)
    assert p.position_i[0] == 10

def test_Particle_update_position_clamp_lower():
    import pso_simple
    pso_simple.num_dimensions = 2
    p = Particle([-9.0, 0.0])
    p.velocity_i = [-5.0, 0.0]
    bounds = [(-10, 10), (-10, 10)]
    p.update_position(bounds)
    assert p.position_i[0] == -10


# === minimize tests ===

def test_minimize_returns_tuple():
    err, pos = minimize(sphere, [5.0, 5.0], [(-10, 10), (-10, 10)], num_particles=5, maxiter=10)
    assert isinstance(err, float)
    assert isinstance(pos, list)
    assert len(pos) == 2

def test_minimize_converges():
    err, pos = minimize(sphere, [5.0, 5.0], [(-10, 10), (-10, 10)], num_particles=20, maxiter=100)
    assert err < 1.0  # Should get close to 0

def test_minimize_respects_bounds():
    err, pos = minimize(sphere, [5.0], [(-2, 2)], num_particles=10, maxiter=50)
    assert -2 <= pos[0] <= 2

def test_minimize_single_dimension():
    err, pos = minimize(sphere, [5.0], [(-10, 10)], num_particles=10, maxiter=50)
    assert len(pos) == 1
    assert err < 5.0
