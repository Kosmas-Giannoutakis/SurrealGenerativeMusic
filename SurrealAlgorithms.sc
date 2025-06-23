SurrealAlgorithms {
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

	// 2. Random gap selection with standard midpoint
	// Randomly selects gaps between existing points and places new values at their exact midpoints. This creates a uniform subdivision pattern with stochastic selection, maintaining balanced spacing while introducing randomness in the order of refinement.
    *randomGapMidpoint { |steps = 1000, chronological = true|
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
		var chosenIndex, interval, primes, temp;
		var cycle, baseRatio, goldenRatio, minRatio, maxRatio;

		// First 50 primes for variety
		primes = [
			2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
			73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
			157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229
		];
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

}
