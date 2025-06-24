SurrealAlgorithms {

	classvar <primes;

	*initClass {
        "Initializing SurrealAlgorithms class variables.".postln;
        primes = [
            2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
            73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
            157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233,
            239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317,
            331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419,
            421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503,
            509, 521, 523, 541
        ];
    }

    // Helper method: Efficient binary search (no changes needed)
    *findInsertionIndex { |array, value|
        var low, high, mid;
        low = 0;
        high = array.size - 1;
        while { low <= high } {
            mid = (low + high) >> 1;
            if (array[mid] < value) {
                low = mid + 1;
            } {
                high = mid - 1;
            };
        };
        ^low
    }

	*pAdicValuation { |depth, p|
		var val = 0;
		var n = depth;
		while { n > 0 and: { n mod: p == 0 } } {
			val = val + 1;
			n = n div: p;
		};
		^val;
	}

	// 1. Conway's original construction of surreal numbers
	// The authentic surreal number construction following John Conway's definition. Generates numbers by generation, where each generation contains all dyadic rationals with denominators that are powers of 2. Each new generation doubles the density of numbers between 0 and 1.
	*conwayConstruction { |steps = 100, chronological = true|
		var sequence, generation = 0;
		var palette;
		sequence = [];

		// Safety limit to prevent massive generations
		if(steps > 100000) {
			"conwayConstruction: steps limited to 100000".warn;
			steps = 100000;
		};

		while { sequence.size < steps } {
			var denominator, needed, count, newValues;
			needed = steps - sequence.size;
			denominator = 1 << (generation + 1);  // Integer bit-shift
			count = denominator >> 1;  // Number of values in this generation

			// For large generations: generate only needed values without full array
			if(count > 100000 && (needed < count)) {
				var used = Set();  // Track used indices to avoid duplicates
				newValues = [];
				while { newValues.size < needed } {
					var index = count.rand;  // Random index in 0..count-1
					if(used.includes(index).not) {
						var num;
						used.add(index);
						// Calculate odd numerator: 2*index + 1
						num = 2 * index + 1;
						newValues = newValues.add(num / denominator);
					};
				};
			}
			{  // For smaller generations: generate full set and take needed
				var numerators;
				if(denominator <= 65536) {
					numerators = (1, 3 .. denominator - 1);
				} {
					numerators = Array.fill(count, { |i| 2*i + 1 });
				};
				newValues = (numerators / denominator).scramble.keep(needed);
			};

			sequence = sequence ++ newValues;
			generation = generation + 1;
		};

		if(chronological.not) {
			palette = ([0.0, 1.0] ++ sequence).sort;
		};

		^if(chronological, sequence, palette);
	}


	// 1.5 Conway's original construction (True Implementation)
	// NOTE: This Surreal-based algorithm is provided as a theoretical exercise.
	// Due to the combinatorial explosion inherent in pure Surreal arithmetic,
	// this method becomes computationally intractable and will fail after a
	// very small number of steps (typically < 5).
	// For practical use, please use the fast, Float-based version.
	*conwayConstructionSurreal { |generationLimit = 4|
		var numbers = Set.with(Surreal.zero);
		var newNumbers, allNumbers, minVal, maxVal;

		generationLimit.do { |g|
			"Generating generation " ++ (g + 1) ++ "...".postln;
			newNumbers = Set.new;
			allNumbers = numbers.asArray;

			// Step 1: Create numbers {x | y} for all adjacent pairs x < y.
			allNumbers.do { |x|
				allNumbers.do { |y|
					if (x < y) {
						// Check if there is no existing number z such that x < z < y
						var noNumberBetween = allNumbers.every({ |z|
							(z <= x) or: { z >= y }
						});
						if (noNumberBetween) {
							newNumbers.add(Surreal.new([x], [y]));
						}
					}
				}
			};

			// Step 2: Find the current minimum and maximum numbers.
			// The default implementation of minItem/maxItem uses the '<' operator,
			// which is correctly defined for the Surreal class.
			minVal = allNumbers.minItem;
			maxVal = allNumbers.maxItem;

			// Step 3: Create the new extremal numbers { | min } and { max | }.
			// This generates the next negative and positive integers.
			newNumbers.add(Surreal.new([], [minVal]));
			newNumbers.add(Surreal.new([maxVal], []));

			// Step 4: Add all newly created numbers to the main set.
			numbers.addAll(newNumbers);
		};

		// Return the final, sorted list of Surreal objects.
		// The default Array.sort uses the '<' method.
		^numbers.asArray.sort;
	}

	// 2. Random gap selection with standard midpoint
	// Randomly selects gaps between existing points and places new values at their exact midpoints. This creates a uniform subdivision pattern with stochastic selection, maintaining balanced spacing while introducing randomness in the order of refinement.
    *randomGapMidpoint { |steps = 100, chronological = true|
        var palette, sequence, index, left, right, v;
        palette = [0.0, 1.0];
        sequence = []; // Array to store the historical sequence

        steps.do { |iter|
            index = (palette.size - 1).rand;
            left = palette[index];
            right = palette[index + 1];
            v = (left + right) * 0.5;

            sequence = sequence.add(v); // Store the new value
            palette = palette.insert(index + 1, v); // Update the sorted palette
        };
        ^if(chronological, sequence, palette) // Return based on the argument
    }

	// 2.5 Random Gap Selection with Standard Midpoint (Surreal Version)
	// Creates exact dyadic rationals at each step.
	// NOTE: This Surreal-based algorithm is provided as a theoretical exercise.
	// Due to the combinatorial explosion inherent in pure Surreal arithmetic,
	// this method becomes computationally intractable and will fail after a
	// very small number of steps (typically < 5).
	// For practical use, please use the fast, Float-based version.
	*randomGapMidpointSurreal { |steps = 4, chronological = true|
		var palette, sequence, index, left, right, v;
		var half = Surreal.dyadic(1, 2); // Pre-calculate 1/2 for efficiency

		// Start with Surreal zero and one
		palette = [Surreal.zero, Surreal.one];
		sequence = [];

		steps.do { |iter|
			index = (palette.size - 1).rand;
			left = palette[index];
			right = palette[index + 1];

			// Perform arithmetic using the Surreal class.
			v = (left + right) * half;

			sequence = sequence.add(v);
			// Palette is guaranteed to remain sorted by inserting at index+1
			palette = palette.insert(index + 1, v);
		};

		if (chronological) {
			^sequence // Returns an Array of Surreal objects
		} {
			^palette  // The sorted palette of Surreal objects
		}
	}

	// 3. Binary Heap (Priority-Based)
	// Uses a priority queue system where intervals are weighted by their size and depth. Larger, shallower intervals get higher priority for subdivision. Creates a balanced tree-like refinement pattern that naturally focuses on the most significant gaps first.
	*binaryHeap { |steps = 100, chronological = true|
		var intervals, sequence, current, left, right, depth, mid;
		var priority, leftPriority, rightPriority, counter = 0;

		// Start with the initial [0,1] interval
		intervals = List[
			[1.0, 0.0, 1.0, 0]  // [priority, left, right, depth]
		];
		sequence = List.new;

		while { sequence.size < steps and: { intervals.size > 0 } } {
			// Find interval with highest priority
			priority = -1;
			current = nil;
			intervals.do { |item|
				if (item[0] > priority) {
					priority = item[0];
					current = item;
				};
			};

			// Remove the selected interval
			intervals.remove(current);

			#priority, left, right, depth = current;
			mid = (left + right) * 0.5;

			// Add the midpoint to our sequence
			sequence.add(mid);

			// Create new intervals if needed
			if (sequence.size < steps) {
				// Left sub-interval
				leftPriority = (mid - left) * (0.9 ** depth);
				intervals.add([leftPriority, left, mid, depth + 1]);

				// Right sub-interval
				rightPriority = (right - mid) * (0.9 ** depth);
				intervals.add([rightPriority, mid, right, depth + 1]);
			};
		};

		// Return appropriate output
		if (chronological) {
			^sequence.array
		} {
			^([0.0, 1.0] ++ sequence).sort
		}
	}

	// 3.5 Binary Heap (Priority-Based) (Surreal Version)
	// We use the fast float-based comparison for the priority queue, as the exact ordering of priorities is less important than performance.
	// NOTE: This Surreal-based algorithm is provided as a theoretical exercise.
	// Due to the combinatorial explosion inherent in pure Surreal arithmetic,
	// this method becomes computationally intractable and will fail after a
	// very small number of steps (typically < 5).
	// For practical use, please use the fast, Float-based version.
	*binaryHeapSurreal { |steps = 4, chronological = true|
		var intervals, sequence, current, left, right, depth, mid;
		var priority, leftPriority, rightPriority;
		var half = Surreal.dyadic(1, 2);

		// [priority(Float), left(Surreal), right(Surreal), depth(Int)]
		intervals = List[
			[1.0, Surreal.zero, Surreal.one, 0]
		];
		sequence = List.new;

		while { sequence.size < steps and: { intervals.size > 0 } } {
			// --- THE FIX IS HERE ---
			// Find the interval (item) for which the priority (item[0]) is the maximum.
			current = intervals.maxItem({ |item| item[0] });

			intervals.remove(current);

			#priority, left, right, depth = current;

			// Surreal arithmetic for the midpoint
			mid = (left + right) * half;
			sequence.add(mid);

			if (sequence.size < steps) {
				// We use toFloat for priority calculation to avoid slow Surreal subtraction.
				leftPriority = (mid.toFloat - left.toFloat) * (0.9 ** depth);
				intervals.add([leftPriority, left, mid, depth + 1]);

				rightPriority = (right.toFloat - mid.toFloat) * (0.9 ** depth);
				intervals.add([rightPriority, mid, right, depth + 1]);
			};
		};

		if (chronological) {
			^sequence.asArray
		} {
			// Sorting at the end requires Surreal's `<` operator.
			^([Surreal.zero, Surreal.one] ++ sequence).sort; // .sort uses '<' by default
		}
	}

    // 4. Pendulum Gap
	// Alternates between two opposing strategies: even iterations target the largest gap for subdivision, while odd iterations target the smallest gap. This creates a pendulum-like behavior that balances between coarse and fine detail, preventing excessive clustering.
	*generatePendulumGap { |steps = 1000, chronological = true|
		var palette, sequence, i, gap, left, right, v;
		var maxGap, maxIndex, minGap, minIndex, minIndices;
		palette = [0.0, 1.0];
		sequence = [];

		steps.do { |iter|
			// On even steps, find the LARGEST gap
			if(iter % 2 == 0) {
				maxGap = -1; maxIndex = 0; i = 0;
				while { i < (palette.size - 1) } {
					gap = palette[i+1] - palette[i];
					if (gap > maxGap) { maxGap = gap; maxIndex = i; };
					i = i + 1;
				};
				left = palette[maxIndex];
				right = palette[maxIndex + 1];
				v = (left + right) * 0.5;
				palette = palette.insert(maxIndex + 1, v);
			} {
				// On odd steps, find the SMALLEST gap (with random tie-break)
				minGap = 2; minIndices = []; i = 0;
				while { i < (palette.size - 1) } {
					gap = palette[i+1] - palette[i];
					if (gap < minGap) { minGap = gap; };
					i = i + 1;
				};
				i = 0;
				while { i < (palette.size - 1) } {
					gap = palette[i+1] - palette[i];
					if ((gap - minGap).abs < 1e-9) { minIndices = minIndices.add(i); };
					i = i + 1;
				};
				minIndex = minIndices.choose;
				left = palette[minIndex];
				right = palette[minIndex + 1];
				v = (left + right) * 0.5;
				palette = palette.insert(minIndex + 1, v);
			};
			sequence = sequence.add(v);
		};
		^if(chronological, sequence, palette);
	}

	// 4.5 Pendulum Gap (Surreal Version)
	// This algorithm is based on comparing gap sizes. This will be very slow with pure Surreal arithmetic (`right - left`). We make the practical compromise of using `toFloat` to find the min/max gaps, but use pure `Surreal` arithmetic to calculate the new midpoint.
	// NOTE: This Surreal-based algorithm is provided as a theoretical exercise.
	// Due to the combinatorial explosion inherent in pure Surreal arithmetic,
	// this method becomes computationally intractable and will fail after a
	// very small number of steps (typically < 5).
	// For practical use, please use the fast, Float-based version.
	*pendulumGapSurreal { |steps = 4, chronological = true|
		var palette, sequence, i, left, right, v;
		var maxGap, maxIndex, minGap, minIndex, minIndices, gap;
		var half = Surreal.dyadic(1, 2);

		palette = [Surreal.zero, Surreal.one];
		sequence = [];

		steps.do { |iter|
			// Even steps: find the LARGEST gap using fast float comparison
			if (iter.even) {
				maxGap = -1; maxIndex = 0; i = 0;
				while { i < (palette.size - 1) } {
					// Practical compromise: use toFloat for gap size comparison
					gap = palette[i+1].toFloat - palette[i].toFloat;
					if (gap > maxGap) { maxGap = gap; maxIndex = i; };
					i = i + 1;
				};
				left = palette[maxIndex];
				right = palette[maxIndex + 1];
				// Pure Surreal arithmetic for the new value
				v = (left + right) * half;
				palette = palette.insert(maxIndex + 1, v);
			} {
				// Odd steps: find the SMALLEST gap using fast float comparison
				minGap = 2.0; minIndices = []; i = 0;
				while { i < (palette.size - 1) } {
					gap = palette[i+1].toFloat - palette[i].toFloat;
					if (gap < minGap) { minGap = gap; };
					i = i + 1;
				};
				i = 0;
				while { i < (palette.size - 1) } {
					gap = palette[i+1].toFloat - palette[i].toFloat;
					if ((gap - minGap).abs < 1e-9) { minIndices = minIndices.add(i); };
					i = i + 1;
				};
				minIndex = minIndices.choose;
				left = palette[minIndex];
				right = palette[minIndex + 1];
				// Pure Surreal arithmetic for the new value
				v = (left + right) * half;
				palette = palette.insert(minIndex + 1, v);
			};
			sequence = sequence.add(v);
		};

		if (chronological) {
			^sequence
		} {
			^palette
		}
	}

	// 5. Probabilistic Max Gap
	// Weights gap selection probability by gap size raised to a power (bias parameter). Larger gaps have exponentially higher chances of being selected, but smaller gaps still have non-zero probability. Creates organic, non-deterministic refinement patterns.

	*probabilisticMaxGap { |steps = 100, bias = 1.0, chronological = true|
		var palette, sequence, gaps, weights, chosenIndex, left, right, v;
		palette = [0.0, 1.0];
		sequence = [];

		steps.do { |iter|
			gaps = palette.differentiate.drop(1);

			if (gaps.size == 1) {
				chosenIndex = 0;
			} {
				// <<< THE KEY CHANGE >>>
				// Weights are now DIRECTLY proportional to gap size.
				// A higher bias means a stronger preference for the largest gaps.
				weights = gaps.pow(bias);
				chosenIndex = weights.windex;
			};

			left = palette[chosenIndex];
			right = palette[chosenIndex + 1];
			v = (left + right) * 0.5;

			sequence = sequence.add(v);
			palette = palette.insert(chosenIndex + 1, v);
		};
		^if(chronological, sequence, palette);
	}

    // 6. Bia Midpoint
	// Similar to random gap selection, but instead of placing new points at exact midpoints, uses a 3:1 left-biased ratio (75% toward left, 25% toward right). This creates asymmetric subdivision patterns with leftward drift.
    *biasMidpoint { |steps = 1000, chronological = true|
        var palette, sequence, index, left, right, v;
        palette = [0.0, 1.0];
        sequence = [];

        steps.do { |iter|
            index = (palette.size - 1).rand;
            left = palette[index];
            right = palette[index + 1];
            v = (left * 0.75) + (right * 0.25);

            sequence = sequence.add(v);
            palette = palette.insert(index + 1, v);
        };
        ^if(chronological, sequence, palette)
    }

    // 7. Random Pair Interpolation
	// Selects two random existing points and creates a new point by weighted interpolation between them. Unlike gap-based methods, this can create points anywhere in the range, leading to more chaotic and less structured patterns.
	*randomPairInterpolation { |steps = 1000, chronological = true|
		var palette, sequence, indices, a, b, v, insertIndex;
		var weight;
		palette = [0.0, 1.0];
		sequence = [];

		steps.do { |iter|
			indices = [0, 0];
			while { indices[0] == indices[1] } {
				indices = [palette.size.rand, palette.size.rand];
			};

			// Ensure a is always the smaller number to keep things simple
			a = palette[indices.minItem];
			b = palette[indices.maxItem];

			// <<< THE FIX: Interpolate instead of adding >>>
			// Pick a random weight between 0 and 1.
			weight = rrand(0.0, 1.0);
			// Calculate a weighted average. This is guaranteed to be between a and b.
			v = (a * weight) + (b * (1 - weight));

			sequence = sequence.add(v);

			// Since v is guaranteed to be between a and b, and the palette is sorted,
			// we can still use findInsertionIndex to place it correctly.
			insertIndex = this.findInsertionIndex(palette, v);
			palette = palette.insert(insertIndex, v);
		};
		^if(chronological, sequence, palette)
	}

    // 8. Grounded Midpoint
	// Random gap selection with probabilistic "anchoring" to fundamental values. With a given probability, new points are placed at half the left boundary value instead of the gap midpoint. This creates gravitational pulls toward zero and other fundamental anchors.

	*groundedMidpoint { |steps = 1000, anchorProb = 0.2, chronological = true|
		var palette, sequence, index, left, right, v;
		var insertIndex; // Variable to hold the result of our search
		palette = [0.0, 1.0];
		sequence = [];

		steps.do { |iter|
			index = (palette.size - 1).rand;
			left = palette[index];
			right = palette[index + 1];
			v = if (anchorProb.coin) {
				left * 0.5
			} {
				(left + right) * 0.5
			};

			sequence = sequence.add(v);

			// --- PERFORMANCE OPTIMIZATION ---
			// Instead of this slow line:
			// palette = (palette.add(v)).sort;

			// Use this much faster approach:
			insertIndex = this.findInsertionIndex(palette, v);
			palette = palette.insert(insertIndex, v);
		};

		^if(chronological, sequence, palette)
	}

	// 9. Depth-Weighted Midpoint Selection
	// Tracks the "generation depth" of each point and uses this in gap selection. Gaps are weighted by both size and the relative depths of their boundaries. Points with different generation depths receive biased subdivision ratios, creating hierarchical branching patterns.
	*depthWeighted { |steps=100, chronological=true, gapBias=1.0, depthBias=0.5|
		var palette = [0.0, 1.0];
		var depths = [0, 0]; // Track creation depth
		var sequence = [];

		steps.do { |iter|
			var gaps, weights, chosenIndex, left, right, d1, d2, f, v, newDepth;

			// Calculate gaps between consecutive values
			gaps = palette.differentiate.drop(1);
			weights = gaps.pow(gapBias); // Weight by gap size
			chosenIndex = weights.windex; // Choose gap probabilistically

			// Get adjacent values and their depths
			left = palette[chosenIndex];
			right = palette[chosenIndex + 1];
			d1 = depths[chosenIndex];
			d2 = depths[chosenIndex + 1];

			// Calculate depth-biased fraction with randomness
			f = if(d1 == d2) {
				0.5 + 0.3.rand2 // Random offset when depths equal
			} {
				0.5 + depthBias * (d1 - d2).sign * 0.2.rand
			}.clip(0.1, 0.9);

			v = (left * (1 - f)) + (right * f);
			newDepth = max(d1, d2) + 1; // New depth = max neighbor + 1

			sequence = sequence.add(v);
			palette = palette.insert(chosenIndex + 1, v);
			depths = depths.insert(chosenIndex + 1, newDepth);
		};

		^if(chronological, sequence, palette)
	}

	// 10. Prime Harmonic Oscillator Generator (PHOGen)
	// Uses prime numbers to modulate subdivision ratios through trigonometric functions. Each interval is assigned a prime index, and the split ratio oscillates based on harmonic relationships between primes. Creates complex, mathematically structured patterns with musical overtones.
	*primeHarmonicOscillator { |steps=100, chronological=true, phaseStep=0.1, harmonicScale=0.5|
		var sequence, intervals, current, left, right, depth;
		var primeIndex, prime, harmonicRatio, angle, splitRatio;
		var mid, newDepth, i, maxPriority, priority, gap;
		var chosenIndex, interval, temp;
		var cycle, baseRatio, goldenRatio, minRatio, maxRatio;

		sequence = List.new;
		intervals = List[[0.0, 1.0, 0, 0]];  // [left, right, depth, primeIndex]
		cycle = 0;
		goldenRatio = 1.61803398875;  // Defined here

		while { sequence.size < steps and: { intervals.size > 0 } } {
			// Find interval with highest priority
			maxPriority = -1;
			chosenIndex = nil;
			i = 0;
			while { i < intervals.size } {
				interval = intervals[i];
				#left, right, depth, primeIndex = interval;
				gap = right - left;
				prime = primes[primeIndex % primes.size];

				// Prioritize larger gaps with depth decay
				priority = gap * (prime ** harmonicScale) / (depth + 1);

				if (priority > maxPriority) {
					maxPriority = priority;
					chosenIndex = i;
				};
				i = i + 1;
			};

			// Process selected interval
			current = intervals.removeAt(chosenIndex);
			#left, right, depth, primeIndex = current;
			prime = primes[primeIndex % primes.size];

			// Create varied base ratio
			baseRatio = 1.0 / (prime % 7 + 1);  // Varies between 1/1 to 1/7
			baseRatio = baseRatio * (1.0 / goldenRatio);  // Scale by golden ratio

			// Trigonometric modulation
			angle = (depth + cycle) * phaseStep * 2pi;
			splitRatio = baseRatio * (1.0 + 0.5 * sin(angle));

			// Dynamic range based on depth
			minRatio = 0.1 + (0.4 * (depth / (depth + 5)));
			maxRatio = 0.9 - (0.4 * (depth / (depth + 5)));
			splitRatio = splitRatio.clip(minRatio, maxRatio);

			// Calculate new point
			mid = left + (right - left) * splitRatio;
			sequence.add(mid);

			// Create new intervals with rotated primes
			newDepth = depth + 1;
			intervals.add([left, mid, newDepth, (primeIndex + 1) % primes.size]);
			intervals.add([mid, right, newDepth, (primeIndex + (primes.size div: 2)) % primes.size]);

			// Cycle counter to prevent repetition
			cycle = (cycle + 1) % 100;
		};

		// Return results
		if (chronological) {
			^sequence.array
		} {
			temp = [0.0, 1.0] ++ sequence;
			^temp.sort
		}
	}

	// 11. Golden Ratio Split
	// Rotates between multiple golden ratio-based split ratios (0.382, 0.5, 0.618) while always targeting the largest available gap. Combines the mathematical elegance of golden ratio proportions with systematic gap reduction, creating aesthetically pleasing distributions.
	*goldenCascade { |steps=100, chronological=true|
		var palette = [0.0, 1.0];
		var sequence = [];
		var ratios = [0.382, 0.5, 0.618]; // Multiple golden ratios
		var nextRatio = 0;

		steps.do { |iter|
			var gaps, maxGap, maxIndex, left, right, v;

			// Rotate through different split ratios
			var ratio = ratios[nextRatio];
			nextRatio = (nextRatio + 1) % ratios.size;

			// Find largest gap
			gaps = palette.differentiate.drop(1);
			maxGap = gaps.maxItem;
			maxIndex = gaps.indexOf(maxGap);

			// Split using current ratio
			left = palette[maxIndex];
			right = palette[maxIndex + 1];
			v = left + (maxGap * ratio);

			sequence = sequence.add(v);
			palette = palette.insert(maxIndex + 1, v);
		};

		^if(chronological, sequence, palette)
	}

	// 11.5 Golden Ratio Split (Surreal Version)
	// Similar to Pendulum Gap, we find the largest gap using floats for speed, but calculate the split using exact Surreal arithmetic. This algorithm produces dyadic rationals if the ratios themselves are dyadic.
	// NOTE: This Surreal-based algorithm is provided as a theoretical exercise.
	// Due to the combinatorial explosion inherent in pure Surreal arithmetic,
	// this method becomes computationally intractable and will fail after a
	// very small number of steps (typically < 5).
	// For practical use, please use the fast, Float-based version.
	*goldenCascadeSurreal { |steps = 1, chronological = true|
		var palette, sequence, maxGap, maxIndex;
		var left, right, v, gapsAsFloats;

		// --- FIX IS HERE ---
		// REMOVE the faulty 'ratios' array.
		// USE this 'goldenRatios' array, which contains valid dyadic approximations.
		var goldenRatios = [
			Surreal.dyadic(98, 256),   // Approximation of 0.382 (~0.3828)
			Surreal.dyadic(1, 2),        // Exactly 0.5
			Surreal.dyadic(158, 256)   // Approximation of 0.618 (~0.6171)
		];
		var nextRatioIndex = 0;

		palette = [Surreal.zero, Surreal.one];
		sequence = [];

		steps.do { |iter|
			// Use the correct array here
			var ratio = goldenRatios[nextRatioIndex];
			nextRatioIndex = (nextRatioIndex + 1) % goldenRatios.size;

			// Find largest gap using fast float comparison
			gapsAsFloats = palette.differentiate.drop(1).collect({ |surrealGap|
				surrealGap.toFloat // This is a practical compromise
			});
			maxGap = gapsAsFloats.maxItem;
			maxIndex = if(maxGap.isNil) { 0 } { gapsAsFloats.indexOf(maxGap) };

			left = palette[maxIndex];
			right = palette[maxIndex + 1];

			// Calculate the split using pure Surreal arithmetic
			v = left + ((right - left) * ratio);

			sequence = sequence.add(v);
			palette = palette.insert(maxIndex + 1, v);
		};

		if (chronological) {
			^sequence
		} {
			^palette
		}
	}

	// 12. Entropic Gap
	// Maintains a memory buffer of recently modified gaps and boosts the selection probability of neighboring gaps. This creates clustering effects where subdivision activity tends to spread to nearby regions, simulating entropy-like diffusion processes.
	*entropicGap { |steps=100, chronological=true, memory=3|
		var palette = [0.0, 1.0];
		var sequence = [];
		var recentIndices = []; // Track recently split gaps

		steps.do { |iter|
			var gaps, weights, chosenIndex, left, right, v;

			gaps = palette.differentiate.drop(1);
			weights = gaps.copy; // Start with gap sizes

			// Boost weights near recently modified gaps
			recentIndices.do { |idx|
				// Only consider valid indices
				var neighborIndices = [idx-1, idx, idx+1].select { |i|
					(i >= 0) && (i < weights.size)
				};

				neighborIndices.do { |i|
					weights[i] = weights[i] * 1.5
				};
			};

			chosenIndex = weights.windex;
			left = palette[chosenIndex];
			right = palette[chosenIndex + 1];
			v = (left + right) * 0.5;

			sequence = sequence.add(v);
			palette = palette.insert(chosenIndex + 1, v);

			// Update recent indices (FIFO queue)
			recentIndices = recentIndices.add(chosenIndex).keep(-1 * memory);
		};

		^if(chronological, sequence, palette)
	}

	// 13. Trigonometric Surreal
	// Uses sine wave modulation to vary split ratios over time and depth. The trigonometric phase evolves with each iteration, creating wave-like patterns in the point distribution. Depth information adds harmonic complexity to the base oscillation.
	*trigonometricSurreal { |steps=100, chronological=true, phaseStep=0.1, frequency=1.0, amplitude=0.3|
		var palette, sequence, phase, maxGap, maxIndex, i;
		var gap, left, right, depth, baseRatio, trigRatio, v;
		var depths, depthMap, maxDepth;

		// Initialize
		palette = [0.0, 1.0];
		sequence = [];
		depths = [0, 0];  // Track generation depth for each point
		phase = 0.0;
		depthMap = Dictionary.new;  // Track depth for each interval

		// Store initial interval depth
		depthMap.put([0.0, 1.0], 0);

		steps.do { |iter|
			// Find largest gap
			maxGap = -1;
			maxIndex = 0;
			i = 0;
			while { i < (palette.size - 1) } {
				gap = palette[i+1] - palette[i];
				if (gap > maxGap) {
					maxGap = gap;
					maxIndex = i;
				};
				i = i + 1;
			};

			left = palette[maxIndex];
			right = palette[maxIndex + 1];

			// Get depth of current interval
			depth = depthMap.at([left, right]) ? 0;

			// Calculate trigonometric ratio
			trigRatio = 0.5 * (1.0 + amplitude * sin(phase + (depth * frequency * 2pi)));

			// Apply depth-based adjustment to avoid convergence
			baseRatio = 0.5 * (1.0 + (0.3 * sin(iter * 0.1)));
			v = left + (maxGap * (baseRatio * trigRatio).clip(0.2, 0.8));

			// Add new value
			sequence = sequence.add(v);
			palette = palette.insert(maxIndex + 1, v);

			// Update depth tracking
			depths = depths.insert(maxIndex + 1, depth + 1);

			// Store depths for new intervals
			depthMap.put([left, v], depth + 1);
			depthMap.put([v, right], depth + 1);

			// Evolve phase
			phase = phase + phaseStep;
		};

		if(chronological) {
			^sequence
		} {
			^palette
		}
	}


	// 14. Dyadic Waveform Generator
	// Applies sinusoidal modulation to both gap selection weights and split positions. The waveform creates periodic preferences for certain gaps while simultaneously varying where within each gap the new point is placed, resulting in wave-like density variations.
	*dyadicWave { |steps=100, chronological=true, freq=0.1, ratio=0.618|
		var palette = [0.0, 1.0];
		var sequence = [];
		var phase = 0.0;

		steps.do { |iter|
			var gaps, weights, chosenIndex, left, right, v;

			gaps = palette.differentiate.drop(1);

			// Waveform modulates gap selection probability
			weights = gaps.collect { |gap, i|
				gap * (1 + sin(phase + (i * 0.5)))
			};

			chosenIndex = weights.windex;
			left = palette[chosenIndex];
			right = palette[chosenIndex + 1];

			// Waveform also affects split position
			v = left + ((right - left) * ratio * (1 + sin(phase)).abs);

			sequence = sequence.add(v);
			palette = palette.insert(chosenIndex + 1, v);
			phase = phase + freq;
		};

		^if(chronological, sequence, palette)
	}

	// 15. Complex Spiral
	// Maps the algorithm to the complex plane using polar coordinates. Points are distributed around angular gaps while applying spiral decay to magnitudes. Includes various magnitude modes (average, random blend, max, unit circle) and chaos parameters for organic variation.

	*complexSpiral{
		|steps=100,
		chronological=true,
		spiralFactor=0.98,
		angleOffset=0.0,
		placementRatio=0.5, // New: 0.5 is midpoint, 0.618 is golden ratio
		magnitudeMode=\avg, // New: \avg, \randBlend, \max, \one
		chaos = 0.0 | // New: 0 to 1, amount of randomness

		var palette, sequence, angles, magnitudes;
		var sortedIndices, gaps, gapSize, chosenIndex;
		var leftAngle, leftMag, rightMag, newAngle, newMag, newPoint, value;
		var goldenRatio = 0.61803398875; // Pre-calculate for convenience

		// Allow using the symbol \golden for placementRatio
		if(placementRatio == \golden) { placementRatio = goldenRatio };

		palette = [ Polar(1, 0).asComplex, Polar(1, pi).asComplex ];
		sequence = [];
		angles = [0, pi];
		magnitudes = [1, 1];

		steps.do {
			gaps = [];
			sortedIndices = (0..palette.size-1).sort({ |a, b| angles[a] < angles[b] });

			palette.size.do { |i|
				var current, next, gap;
				current = sortedIndices[i];
				next = sortedIndices[(i + 1) % palette.size];
				gap = (angles[next] - angles[current]).mod(2pi);
				gaps = gaps.add(gap);
			};

			gapSize = gaps.maxItem;
			chosenIndex = gaps.indexOf(gapSize);

			leftAngle = angles[sortedIndices[chosenIndex]];
			leftMag = magnitudes[sortedIndices[chosenIndex]];
			rightMag = magnitudes[sortedIndices[(chosenIndex + 1) % palette.size]];

			// --- ANGLE CALCULATION (IMPROVED) ---
			newAngle = (
				leftAngle
				+ (gapSize * placementRatio) // Use the placement ratio
				+ angleOffset
				+ (chaos * rrand(-0.1, 0.1) * gapSize) // Add chaos
			).mod(2pi);

			// --- MAGNITUDE CALCULATION (IMPROVED) ---
			switch (magnitudeMode)
			{ \avg }{ // Original behavior
				newMag = (leftMag + rightMag) * 0.5;
			}
			{ \randBlend }{ // Randomly blend parent magnitudes
				newMag = leftMag.blend(rightMag, rrand(0.0, 1.0));
			}
			{ \max }{ // Take the larger of the two parents
				newMag = max(leftMag, rightMag);
			}
			{ \one } { // Force to the unit circle (no spiral)
				newMag = 1.0;
			};

			// Apply spiral factor and chaos after the main calculation
			newMag = newMag * spiralFactor * (1 + rrand(chaos * -0.1, chaos * 0.1));
			// Prevent magnitude from becoming negative
			newMag = newMag.max(0);

			newPoint = Polar(newMag, newAngle).asComplex;

			palette = palette.add(newPoint);
			angles = angles.add(newAngle);
			magnitudes = magnitudes.add(newMag);

			value = newAngle / 2pi;
			sequence = sequence.add(value);
		};

		if(chronological) {
			^sequence
		} {
			^angles.collect(_ / 2pi).sort
		}
	}

	// 16. Riemann Zeta Split (Advanced)
	// This algorithm uses the profound connection between the Riemann Zeta function and
	// prime numbers to determine its subdivision logic. This advanced version introduces
	// parameters to create far more complex and varied patterns without randomness.
	//
	// - splitMode: Controls the direction of the split.
	//     - \left: The original behavior, always splitting from the left boundary.
	//     - \alternating: Flips between splitting from the left and right boundaries.
	//     - \prime: Uses a deep property of number theory (prime % 4) to decide
	//       the split direction, creating a complex but deterministic pattern.
	// - s_drift: Causes the 's' parameter to change based on the current prime's
	//   magnitude, preventing simple convergence.
	*riemannZetaSplit { |steps=100, chronological=true, s=2.0, primeOffset=0, splitMode=\prime, s_drift=0.05|
		var palette, sequence, gaps, maxGap, maxIndex, left, right, v;
		var prime, current_s, zetaRatio, finalRatio;

		if (s <= 1.0) {
			Error("The base Riemann Zeta exponent 's' must be greater than 1.").throw;
		};

		palette = [0.0, 1.0];
		sequence = [];

		steps.do { |iter|
			// 1. Find the largest gap
			gaps = palette.differentiate.drop(1);
			maxGap = gaps.maxItem;
			maxIndex = if(maxGap.isNil) { 0 } { gaps.indexOf(maxGap) };

			// 2. Get the current prime
			prime = primes[(iter + primeOffset) % primes.size];

			// 3. Modulate the 's' parameter based on the prime's magnitude
			// This causes the split character to evolve as we use larger primes.
			// The log ensures the drift isn't too extreme.
			current_s = (s + (s_drift * log(prime))).max(1.0001);

			// 4. Calculate the base ratio from the Zeta function's Euler product term
			zetaRatio = 1.0 - (prime.pow(current_s.neg));

			// 5. Determine the final split ratio based on the chosen mode
			finalRatio = switch(splitMode)
			{ \left }{
				// Original behavior: always split relative to the left boundary.
				zetaRatio;
			}
			{ \alternating }{
				// Simple alternation between left-biased and right-biased splits.
				if(iter.even) { zetaRatio } { 1.0 - zetaRatio };
			}
			{ \prime }{
				// Use number theory to decide the direction. Primes (other than 2)
				// are either of the form 4k+1 or 4k+3. This property is used
				// to deterministically flip the split direction.
				if ((prime % 4) == 1) { zetaRatio } { 1.0 - zetaRatio };
			}
			{
				// Default to the prime-based mode if an unknown symbol is given.
				if ((prime % 4) == 1) { zetaRatio } { 1.0 - zetaRatio };
			};

			// 6. Calculate the new value
			left = palette[maxIndex];
			v = left + (maxGap * finalRatio);

			// 7. Update the palette and sequence
			sequence = sequence.add(v);
			palette = palette.insert(maxIndex + 1, v);
		};

		if (chronological) {
			^sequence
		} {
			^palette
		}
	}

	// 17. Zeta-Cantor Hybrid (Advanced Non-Convergent)
	// This algorithm represents the most sophisticated deterministic generator in the
	// suite, designed to produce endlessly varied, non-convergent, and structurally
	// complex sequences. It addresses the convergence traps of previous versions by
	// balancing two competing priorities with tunable biases.
	//
	// Priority = (gap_size ^ gap_bias) * (phi_proximity ^ phi_bias)
	//
	// 1. gap_bias: The weight given to the physical size of a gap.
	// 2. phi_bias: The weight given to a gap's proximity to the Golden Ratio.
	//
	// By tuning these two biases, the algorithm can be made to favor exploring large
	// empty regions, or to focus on creating intricate, self-similar structures,
	// or a dynamic balance of both, ensuring it never gets stuck.
	*zetaCantor {
		|steps=200, chronological=true,
		s=1.1,           // Zeta exponent (controls split asymmetry)
		primeOffset=0,
		gap_bias=1.0,    // How much to prefer large gaps (1.0 is standard)
		phi_bias=0.25,   // How much to prefer gaps near the Golden Ratio
		phi_factor=0.61803398875|

		var palette, sequence, left, right, v;
		var prime, zetaRatio, finalRatio;

		palette = [0.0, 1.0];
		sequence = [];

		steps.do { |iter|
			var gaps, priorities, maxPriority, chosenIndex;

			// 1. Calculate weighted priority scores for all gaps
			gaps = palette.differentiate.drop(1);
			priorities = gaps.collect { |gap, i|
				var midpoint = palette[i] + (gap / 2);
				var phi_proximity, gap_score, phi_score;

				// Score based on gap size
				gap_score = gap.pow(gap_bias);

				// Score based on proximity to phi. Value is 1.0 at phi, falls to 0.0 away.
				phi_proximity = 1.0 - (abs(midpoint - phi_factor) * 2).clip(0.0, 1.0);
				phi_score = phi_proximity.pow(phi_bias);

				// The final priority is the product of the two scores
				gap_score * phi_score;
			};

			// 2. Find the gap with the highest priority score
			maxPriority = priorities.maxItem;
			chosenIndex = if(maxPriority.isNil) { 0 } { priorities.indexOf(maxPriority) };

			// 3. Get the current prime and calculate the Zeta-based split ratio
			prime = primes[(iter + primeOffset) % primes.size];
			zetaRatio = 1.0 - (prime.pow(s.neg));
			finalRatio = if((prime % 4) == 1) { zetaRatio } { 1.0 - zetaRatio };

			// 4. Calculate the new value
			left = palette[chosenIndex];
			right = palette[chosenIndex + 1];
			v = left + ((right-left) * finalRatio);

			// 5. Update the palette and sequence
			sequence = sequence.add(v);
			palette = palette.insert(chosenIndex + 1, v);
		};

		if (chronological) {
			^sequence
		} {
			^palette
		}
	}

}
