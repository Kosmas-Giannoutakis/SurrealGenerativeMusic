SurrealGenerator {
    *build { |algorithm, steps, ugenMethod, interpolation, freq, phase, min, max, curve|
        var buf, index, read, mapped, numFrames, rate, sampleRate, phaseFrames;
		var palette = switch(algorithm)
		{1} {SurrealAlgorithms.conwayConstruction(steps)}
		{2} {SurrealAlgorithms.generatePendulumGap(steps)}
		{3} {SurrealAlgorithms.goldenCascade(steps)}
		{4} {SurrealAlgorithms.randomGapMidpoint(steps)}
		{5} {SurrealAlgorithms.biasMidpoint(steps)}
		{6} {SurrealAlgorithms.groundedMidpoint(steps)}
		{7} {SurrealAlgorithms.randomPairInterpolation(steps)}
		{8} {SurrealAlgorithms.probabilisticMaxGap(steps)}
		{9} {SurrealAlgorithms.binaryHeap(steps)}
		{10} {SurrealAlgorithms.entropicGap(steps)}
		{11} {SurrealAlgorithms.depthWeighted(steps)}
		{12} {SurrealAlgorithms.riemannZetaSplit(steps)}
		{13} {SurrealAlgorithms.zetaCantor(steps)}
		{14} {SurrealAlgorithms.trigonometricSurreal(steps)}
		{15} {SurrealAlgorithms.dyadicWave(steps)}
		{16} {SurrealAlgorithms.primeHarmonicOscillator(steps)}
		{17} {SurrealAlgorithms.complexSpiral(steps)};

        buf = LocalBuf(palette.size);
        buf.set(palette);

        numFrames = BufFrames.kr(buf);
        sampleRate = if(ugenMethod == \ar) { SampleRate.ir } { ControlRate.ir };
        rate = if(ugenMethod == \ar) { numFrames * freq / sampleRate } { freq / sampleRate };

        // Apply phase offset in terms of buffer frames (phase range 0 to 1)
        phaseFrames = phase.wrap(0, 1) * numFrames;

        index = Phasor.perform(ugenMethod, 0, rate, 0, numFrames) + phaseFrames;
        index = index % numFrames; // wrap index safely

        read = BufRd.perform(ugenMethod, 1, buf, index, 1, interpolation);
        mapped = [min, max, curve].asSpec.map(read);
        ^mapped;
    }

    *kr {|algorithm=1, steps=200, freq=1, phase=0, min=0, max=1, curve=\lin, interpolation=1|
        ^this.build(algorithm, steps, \kr, interpolation, freq, phase, min, max, curve)
    }

    *ar {|algorithm=1, steps=200, freq=1, phase=0, min= -1, max=1, curve=\lin, interpolation=2|
        ^this.build(algorithm, steps, \ar, interpolation, freq, phase, min, max, curve)
    }
}
