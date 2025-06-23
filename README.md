      
# Surreal.sc
### Surreal Numbers and Generative Algorithms for SuperCollider

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

**Surreal.sc** is a dual-purpose SuperCollider library. It provides:

1.  A formal implementation of **John Conway's Surreal Numbers**, allowing for the creation and manipulation of integers, dyadic rationals, and other exotic numbers within the recursive `{L|R}` structure.
2.  A powerful creative toolkit, `SurrealGenerator`, which uses the principles of surreal construction as a launchpad for **15 unique generative algorithms**. These algorithms can create complex, musical sequences for use in real-time synthesis and composition.

This project is for mathematicians, computer musicians, and creative coders who are interested in exploring the boundary between abstract mathematics and generative art.

## Table of Contents
- [What are Surreal Numbers?](#what-are-surreal-numbers)
- [Installation](#installation)
- [Part 1: Using the `Surreal` Class](#part-1-using-the-surreal-class)
  - [Creating Numbers](#creating-numbers)
  - [Arithmetic and Comparison](#arithmetic-and-comparison)
- [Part 2: Generative Music with `SurrealGenerator`](#part-2-generative-music-with-surrealgenerator)
  - [Control Rate (`.kr`) Example](#control-rate-kr-example)
  - [Audio Rate (`.ar`) Example](#audio-rate-ar-example)
- [The Combinatorial Explosion: Your Generative "Cookbook"](#the-combinatorial-explosion-your-generative-cookbook)
- [The Generative Algorithms in Detail](#the-generative-algorithms-in-detail)
  - [Tier 1: Foundational & Deterministic](#tier-1-foundational--deterministic)
  - [Tier 2: Simple Stochastic](#tier-2-simple-stochastic)
  - [Tier 3: Probabilistic & Weighted](#tier-3-probabilistic--weighted)
  - [Tier 4: State-Driven & Hierarchical](#tier-4-state-driven--hierarchical)
  - [Tier 5: Advanced Mathematical & Modulated](#tier-5-advanced-mathematical--modulated)
- [Contributing](#contributing)
- [License](#license)


## What are Surreal Numbers?

Invented by John Horton Conway, Surreal Numbers are a vast number system built from the simplest possible rules. Every surreal number `x` is defined by two sets of previously created surreal numbers, a Left set `L` and a Right set `R`, written as:

**x = { L | R }**

The fundamental rule is that no member of the Left set can be greater than or equal to any member of the Right set.

This simple recursive definition is powerful enough to construct all integers, dyadic rationals (fractions with a power-of-two denominator like 3/8), and an incredible landscape of other numbers. The number **0** is the simplest, born from empty sets: `{ | }`. From there, we get:
- **1** = `{ 0 | }`
- **-1** = `{ | 0 }`
- **1/2** = `{ 0 | 1 }`

This project leverages this generative, "gap-filling" nature to create musical sequences.

## Installation

1.  Clone this repository.
2.  Move the `Surreal.sc` folder into your SuperCollider `Extensions` directory.
3.  Recompile your class library (`Shift + Cmd + L` on macOS, `Shift + Ctrl + L` on Windows/Linux).

## Part 1: Using the `Surreal` Class

The `Surreal` class allows you to work directly with these numbers.

### Creating Numbers

```supercollider
// Start the interpreter and post the window
s.boot;

// Create integers using the .i convenience method
a = 1.i;
b = 2.i;
a.postln; // -> 1
b.postln; // -> 2

// Create the number zero
z = Surreal.zero;
z.postln; // -> { | }

// Create a dyadic rational (e.g., 3/4)
// {1/2 | 1} -> { {0|1} | { {0|} | } }
d = Surreal.dyadic(3, 4);
d.postln; // -> 3/4

// Create the simplest number between 0 and 1 (which is 1/2)
// using the special 's' method (stands for "star" or "split")
half = 0.i.s(1.i);
half.postln; // -> {0 | 1}
half.asString.postln; // -> 1/2
```
    

Arithmetic and Comparison

Arithmetic is defined recursively, creating new, often very complex surreal numbers.
Generated supercollider

```     
// Addition
c = 1.i + 1.i;
c.postln; // -> 2

// Multiplication
m = 2.i * 3.i;
m.postln; // -> 6

// The raw form of 1/2 + 1/2
// It evaluates to 1, but its internal structure is complex
sum_of_halves = (0.i.s(1.i)) + (0.i.s(1.i));
sum_of_halves.postln; // -> {{1/2 | 3/2}, {1/2 | 3/2}}

// Comparisons work as expected
(sum_of_halves == 1.i).postln; // -> true
(2.i > 1.i).postln; // -> true
(half < 1.i).postln; // -> true
```
    

Part 2: Generative Music with SurrealGenerator

This is the creative heart of the library. SurrealGenerator uses the 15 built-in algorithms to produce number sequences, which are loaded into a buffer and read back as a control or audio signal.
Control Rate (.kr) Example

Here we use an algorithm to modulate the frequency of a sine wave.
Generated supercollider

```      
(
{
    var freq, gen;

    // Use Algorithm #11 (depthWeighted) to generate 100 points
    // Read the sequence back at a rate of 2 Hz
    gen = SurrealGenerator.kr(
        algorithm: 11,
        steps: 100,
        freq: 2,
        min: 200, // map to a frequency range of 200-800 Hz
        max: 800,
        curve: \exp
    );

    // Use the generator to control the frequency
    freq = SinOsc.ar(gen, mul: 0.2);
    // Pan and output the sound
    Pan2.ar(freq, LFNoise0.kr(0.5));
}.play;
)
```
    

Audio Rate (.ar) Example

Here, the generator itself becomes the oscillator's waveform.
Generated supercollider

```      
(
{
    var wave;
    // Use Algorithm #15 (complexSpiral) with 500 steps
    // The frequency of the Phasor reading the buffer is 440 Hz
    wave = SurrealGenerator.ar(
        algorithm: 15,
        steps: 500,
        freq: 440,
        min: -1,
        max: 1
    );

    // The generator *is* the sound
    LeakDC.ar(wave) * 0.4;
}.play;
)
```
    


## The Combinatorial Explosion: Your Generative "Cookbook"

Theoretically, you can invent an infinite number of such algorithms. Think of each algorithm as a recipe with a few key ingredients. By changing any one ingredient or combining them in new ways, you create a new recipe.

The core "ingredients" in your current set are:

-   **The Selection Strategy**: *How do you choose where to add the next point?*
    -   **Deterministic**: Always pick the largest gap, smallest gap, or alternate. (*generatePendulumGap*, *goldenCascade*)
    -   **Random**: Pick any gap with equal probability. (*randomGapMidpoint*)
    -   **Weighted/Probabilistic**: Pick a gap based on a calculated weight (e.g., its size). (*probabilisticMaxGap*)
    -   **Systematic**: Follow a pre-defined, exhaustive system. (*conwayConstruction*)
    -   **Stateful**: The choice depends on the history of previous choices. (*entropicGap*, *depthWeighted*)

-   **The Placement Rule**: *Once a gap (between `left` and `right`) is chosen, how do you calculate the new value `v`?*
    -   **Midpoint**: `(left + right) / 2`
    -   **Biased Point**: `(left * 0.75) + (right * 0.25)`
    -   **Golden Ratio**: `left + (right - left) * 0.618...`
    -   **Random Interpolation**: `left.blend(right, rrand(0.0, 1.0))`
    -   **Modulated**: `left + (right - left) * someFunction(time, depth, etc.)`
    -   **External Anchor**: `left * 0.5` (*groundedMidpoint*)

Since you can mix and match any selection strategy with any placement rule, the number of possible combinations is already large. But it becomes infinite when you realize the functions themselves can be infinitely varied.

## The Generative Algorithms in Detail

The 15 algorithms are ordered by conceptual complexity, from simple and deterministic to complex and modulated.

### Tier 1: Foundational & Deterministic

*These algorithms are non-random and produce predictable, structured output.*

1.  **Conway Construction**
    -   **Description**: The original, canonical method for generating surreal numbers. It exhaustively creates all dyadic rationals generation by generation.
    -   **Selection Strategy**: Systematic. It doesn't "select" gaps but generates all new numbers for a given generation (e.g., all fourths, then all eighths).
    -   **Placement Rule**: Generates all odd numerators over the current power-of-two denominator.

2.  **Pendulum Gap**
    -   **Description**: A deterministic strategy that creates a balanced rhythm by alternating between coarse and fine refinement.
    -   **Selection Strategy**: Deterministic. On even steps, it picks the largest gap. On odd steps, it picks the smallest gap.
    -   **Placement Rule**: Midpoint.

3.  **Golden Cascade**
    -   **Description**: Creates aesthetically pleasing, self-similar distributions by repeatedly splitting the largest gap according to the golden ratio.
    -   **Selection Strategy**: Deterministic. Always picks the largest gap.
    -   **Placement Rule**: Golden Ratio. It cycles through split ratios of 0.382, 0.5, and 0.618.

---

### Tier 2: Simple Stochastic

*These algorithms introduce the element of chance in a straightforward way.*

4.  **Random Gap Midpoint**
    -   **Description**: The simplest stochastic subdivision. Creates a uniform but randomized pattern.
    -   **Selection Strategy**: Random. Picks any available gap with equal probability.
    -   **Placement Rule**: Midpoint.

5.  **Bias Midpoint**
    -   **Description**: A simple variation of the above that introduces asymmetry, causing points to drift.
    -   **Selection Strategy**: Random.
    -   **Placement Rule**: Biased Point (75% towards the left boundary).

6.  **Grounded Midpoint**
    -   **Description**: Creates a "gravitational pull" towards zero by occasionally ignoring the gap and anchoring to the left boundary.
    -   **Selection Strategy**: Random.
    -   **Placement Rule**: Mixed. With a given probability, it's an External Anchor (`left * 0.5`); otherwise, it's a Midpoint.

7.  **Random Pair Interpolation**
    -   **Description**: More chaotic than gap-based methods, as it can create new points anywhere between any two existing points.
    -   **Selection Strategy**: Random Pair. Selects two random, not-necessarily-adjacent points from the entire palette.
    -   **Placement Rule**: Random Interpolation between the two chosen points.

---

### Tier 3: Probabilistic & Weighted

*These algorithms use weighted probability to create more organic, non-uniform patterns.*

8.  **Probabilistic Max Gap**
    -   **Description**: A classic generative algorithm that produces natural-looking distributions by favoring larger gaps.
    -   **Selection Strategy**: Weighted/Probabilistic. The probability of a gap being chosen is proportional to its size.
    -   **Placement Rule**: Midpoint.

---

### Tier 4: State-Driven & Hierarchical

*These methods maintain "memory" or state to influence future decisions, creating evolving structures.*

9.  **Binary Heap**
    -   **Description**: Uses a priority queue to create a balanced, tree-like refinement that focuses on the most significant gaps first.
    -   **Selection Strategy**: Stateful/Priority. Chooses the interval with the highest priority, calculated from its size and generation depth.
    -   **Placement Rule**: Midpoint.

10. **Entropic Gap**
    -   **Description**: Simulates diffusion by creating clusters of activity that spread to neighboring regions.
    -   **Selection Strategy**: Stateful/Weighted. It boosts the selection probability of gaps that are adjacent to recently split gaps.
    -   **Placement Rule**: Midpoint.

11. **Depth Weighted**
    -   **Description**: Creates complex, hierarchical branching by using generation depth to influence both selection and placement.
    -   **Selection Strategy**: Weighted/Probabilistic, based on gap size.
    -   **Placement Rule**: Stateful/Modulated. The split ratio is biased based on the relative depths of the gap's boundary points.

---

### Tier 5: Advanced Mathematical & Modulated

*These algorithms use concepts from higher math to generate intricate, often wave-like patterns.*

12. **Trigonometric Surreal**
    -   **Description**: Creates wave-like patterns by modulating the split point with a sine wave that evolves over time.
    -   **Selection Strategy**: Deterministic. Always picks the largest gap.
    -   **Placement Rule**: Modulated. The split ratio is controlled by a `sin()` function whose phase changes with time and generation depth.

13. **Dyadic Wave**
    -   **Description**: Creates wave-like density variations by using a sine wave to modulate both gap selection and split position.
    -   **Selection Strategy**: Weighted/Modulated. Gap selection probability is weighted by size and a `sin()` function.
    -   **Placement Rule**: Modulated by the same `sin()` function.

14. **Prime Harmonic Oscillator**
    -   **Description**: Generates complex, musically-structured patterns using harmonic relationships between prime numbers to guide subdivision.
    -   **Selection Strategy**: Stateful/Priority. Priority is a complex function of gap size, depth, and an assigned prime number.
    -   **Placement Rule**: Modulated. The split ratio is a function of the assigned prime and a trigonometric phase.

15. **Complex Spiral**
    -   **Description**: The most conceptually distinct algorithm. It operates in the 2D complex plane to create beautiful spiral patterns.
    -   **Selection Strategy**: Deterministic (Angular). Finds the largest *angular* gap between points in polar coordinates.
    -   **Placement Rule**: Polar/Modulated. Calculates a new angle and a new magnitude (which can spiral inwards or outwards) to place the point.

## Contributing

This project is open to new ideas! If you invent a new generative algorithm based on these principles, feel free to submit a pull request. Please add your algorithm to the SurrealAlgorithms class and document its "Selection Strategy" and "Placement Rule."
License

This project is licensed under the MIT License. See the LICENSE file for details.
