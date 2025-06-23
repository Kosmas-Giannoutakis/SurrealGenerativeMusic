SurrealGenerator {
    // A "private" helper method to build the UGen graph.
    // We pass the method (\kr or \ar) and interpolation type as arguments.
    *build { |algorithm, steps, ugenMethod, interpolation, freq, min, max, curve|
        var buf, index, read, mapped, numFrames, rate, sampleRate;
		var palette = switch(algorithm)
		// Tier 1: Foundational & Simple Deterministic
		{1} {SurrealAlgorithms.conwayConstruction(steps)}
		{2} {SurrealAlgorithms.generatePendulumGap(steps)}
		{3} {SurrealAlgorithms.goldenCascade(steps)}
		// Tier 2: Simple Stochastic
		{4} {SurrealAlgorithms.randomGapMidpoint(steps)}
		{5} {SurrealAlgorithms.biasMidpoint(steps)}
		{6} {SurrealAlgorithms.groundedMidpoint(steps)}
		{7} {SurrealAlgorithms.randomPairInterpolation(steps)}
		// Tier 3: Probabilistic & Weighted Selection
		{8} {SurrealAlgorithms.probabilisticMaxGap(steps)}
		// Tier 4: State-Driven & Hierarchical
		{9} {SurrealAlgorithms.binaryHeap(steps)}
		{10} {SurrealAlgorithms.entropicGap(steps)}
		{11} {SurrealAlgorithms.depthWeighted(steps)}
		// Tier 5: Advanced Mathematical & Modulated
		{12} {SurrealAlgorithms.trigonometricSurreal(steps)}
		{13} {SurrealAlgorithms.dyadicWave(steps)}
		{14} {SurrealAlgorithms.primeHarmonicOscillator(steps)}
		{15} {SurrealAlgorithms.complexSpiral(steps)};


        buf = LocalBuf(palette.size);
        buf.set(palette);

        numFrames = BufFrames.kr(buf);

        // Select the correct sample rate based on the method
        sampleRate = if(ugenMethod == \ar) { SampleRate.ir } { ControlRate.ir };

        rate = if(ugenMethod == \ar) { numFrames * freq / sampleRate } { freq/sampleRate };

        // Use .perform to call the correct UGen method (\kr or \ar)
        index = Phasor.perform(ugenMethod, 0, rate, 0, numFrames);
        read = BufRd.perform(ugenMethod, 1, buf, index, 1, interpolation);
        mapped = [min, max, curve].asSpec.map(read);
        ^mapped;
    }

    // The public methods are now simple, one-line calls to the helper.
    *kr {|algorithm=1, steps=200, freq=1, min=0, max=1, curve=\lin, interpolation=1|
        ^this.build(algorithm, steps, \kr, interpolation, freq, min, max, curve)
    }

    *ar {|algorithm=1, steps=200, freq=1, min= -1, max=1, curve=\lin, interpolation=2|
        ^this.build(algorithm, steps, \ar, interpolation, freq, min, max, curve)
    }
}