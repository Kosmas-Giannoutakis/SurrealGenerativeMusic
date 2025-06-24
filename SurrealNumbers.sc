// =================================================================
// Surreal.sc (Definitive Final Version - Corrected)
// =================================================================

Surreal {
    var <l, <r;
    var <hashValue;

    classvar
        <>existingSurreals, <>comparisonCache, <>negationCache,
        <>integerCache, <>integerReverseCache, <>floatCache;

    // ================= CLASS METHODS =================
    *initClass { this.clearCaches; "Surreal Number system initialized.".postln; }

    *clearCaches {
        existingSurreals = Dictionary.new;
        comparisonCache = Dictionary.new;
        negationCache = Dictionary.new;
        integerCache = Dictionary.new;
        integerReverseCache = Dictionary.new;
        floatCache = Dictionary.new;
        this.zero; this.one; this.negOne;
        "Surreal Number caches cleared.".postln;
    }

    *new { |left, right|
        var lArray, rArray, tempHash, bucket, identicalInstance;
        lArray = (left ? []).asArray.sort({ |a, b| a.prCompareByFloat(b) < 0 });
        rArray = (right ? []).asArray.sort({ |a, b| a.prCompareByFloat(b) < 0 });
        tempHash = this.prCalculateHashFor(lArray, rArray);
        bucket = existingSurreals.at(tempHash);
        if (bucket.notNil) {
            identicalInstance = bucket.detect { |existing|
                existing.isIdenticalTo(lArray, rArray)
            };
            if (identicalInstance.notNil) { ^identicalInstance; }
        } {
            bucket = Array.new;
            existingSurreals.put(tempHash, bucket);
        };
        ^this.prNewInternal(lArray, rArray, tempHash, bucket);
    }

    *prNewInternal { |lArray, rArray, aHash, bucket|
        var newInstance = super.new.init(lArray, rArray, aHash);
        bucket.add(newInstance);
        ^newInstance;
    }

	*prSimplifyAndNew { |left, right|
		var maxL, minR, finalL, finalR, maxL_float, minR_float;

		// --- Fast Path (using floats) ---
		if (left.notEmpty) {
			maxL = left.inject(left.first, { |max, elem| if(elem.prCompareByFloat(max) > 0) { elem } { max } });
			maxL_float = maxL.toFloat; // Cache the float value
			finalL = [maxL];
		} {
			finalL = [];
			maxL_float = -inf; // Sentinel value
		};

		if (right.notEmpty) {
			minR = right.inject(right.first, { |min, elem| if(elem.prCompareByFloat(min) < 0) { elem } { min } });
			minR_float = minR.toFloat; // Cache the float value
			finalR = [minR];
		} {
			finalR = [];
			minR_float = inf; // Sentinel value
		};

		// --- Guard Condition ---
		// If the floats are too close, they are untrustworthy.
		// The epsilon (1e-9) is a guess; it may need tuning.
		// Also, if the floats are equal, we must use the pure comparison.
		if ((minR_float - maxL_float) < 1e-9) {
			"WARNING: Float precision limit reached. Falling back to slow, pure comparison for simplification.".postln;
			// --- Slow, Correct Path ---
			if (left.notEmpty) {
				maxL = left.maxItem; // Uses pure comparison
				finalL = [maxL];
			};
			if (right.notEmpty) {
				minR = right.minItem; // Uses pure comparison
				finalR = [minR];
			};
		};

		^this.new(finalL, finalR);
	}

    *prCalculateHashFor { |lArray, rArray|
        var seed = 0;
        seed = (seed * 31) + lArray.size.hash;
        lArray.do { |x| seed = (seed * 31) + x.hash };
        seed = (seed * 31) + rArray.size.hash;
        rArray.do { |x| seed = (seed * 31) + x.hash };
        ^seed;
    }

    *zero { ^this.integer(0) }
    *one { ^this.integer(1) }
    *negOne { ^this.integer(-1) }

    *integer { |val=1|
        var intVal = val.asInteger, result = integerCache.at(intVal);
        if (result.notNil) { ^result };
        result = if (intVal == 0) {
            this.new([], []);
        } { if (intVal > 0) {
            this.new([this.integer(intVal - 1)], []);
        } {
            this.new([], [this.integer(intVal + 1)]);
        } };
        integerCache.put(intVal, result);
        integerReverseCache.put(result.hash, intVal);
        ^result;
    }

    *dyadic { |num, den|
        var n = num.asInteger, d = den.asInteger;
        if (d <= 0) { Error("Denominator must be positive").throw; };
        if (d.isPowerOfTwo.not) { Error("Denominator must be a power of two").throw; };
        while { (n.even) and: { (d > 1) } } { n = n div: 2; d = d div: 2; };
        if (d == 1) { ^this.integer(n) };
        ^this.new([this.dyadic(n - 1, d)], [this.dyadic(n + 1, d)]);
    }

    // ================= INSTANCE METHODS =================
    init { |left, right, aHash|
        l = left; r = right; hashValue = aHash;
        this.prValidate;
        ^this;
    }

	prValidate {
		// Use the non-recursive float-based comparison during validation
		// to break the recursive cycle during object creation.
		l.do { |leftElement|
			r.do { |rightElement|
				if (leftElement.prCompareByFloat(rightElement) >= 0) {
					Error(
						"Invalid Surreal: Found l-element % >= r-element %"
						.format(leftElement.asString, rightElement.asString)
					).throw;
				}
			}
		}
	}

    hash { ^hashValue }

    isIdenticalTo { |otherL, otherR|
        if ( (l.size != otherL.size) or: { r.size != otherR.size } ) { ^false; };
        if (l.every { |item, i| item === otherL[i] }.not) { ^false; };
        if (r.every { |item, i| item === otherR[i] }.not) { ^false; };
        ^true;
    }

    prCompareByFloat { |that|
        ^(this.toFloat - that.toFloat).sign;
    }

	+ { |that|
		var newL, newR;
		that = that.asSurreal;
		if (this.isZero) { ^that }; if (that.isZero) { ^this };

		// --- OPTIMIZATION 1: Integer + Integer ---
		if (this.isInteger and: { that.isInteger }) {
			^Surreal.integer(this.asInteger + that.asInteger);
		};

		// --- OPTIMIZATION 2: Surreal + Integer (The new, crucial optimization) ---
		if (that.isInteger) {
			// x + n = { L(x)+n | R(x)+n }
			// This avoids the massive recursion of the general formula.
			newL = l.collect(_ + that);
			newR = r.collect(_ + that);
			// We still simplify, but the sets are tiny compared to the general case.
			^Surreal.prSimplifyAndNew(newL, newR);
		};
		// Symmetrically, for Integer + Surreal
		if (this.isInteger) {
			^that + this; // Just reuse the logic above by swapping the receiver.
		};

		// --- FALLBACK: The general recursive formula for complex cases ---
		newL = l.collect(_ + that) ++ that.l.collect(this + _);
		newR = r.collect(_ + that) ++ that.r.collect(this + _);
		^Surreal.prSimplifyAndNew(newL, newR);
	}

    - { |that| ^this + that.neg }

    neg {
        var cached = negationCache.at(this.hash), result;
        if (cached.notNil) { ^cached };
        if (this.isInteger) { ^Surreal.integer(this.asInteger.neg) };
        result = Surreal.prSimplifyAndNew(r.collect(_.neg), l.collect(_.neg));
        negationCache.put(this.hash, result);
        negationCache.put(result.hash, this);
        ^result;
    }

	* { |that|
		var newL, newR, x, y;
		that = that.asSurreal;

		// --- BASIC CASES (Corrected) ---
		if (this.isZero or: { that.isZero }) { ^Surreal.zero };
		if (this.isOne) { ^that }; if (that.isOne) { ^this };
		if (this.equals(Surreal.negOne)) { ^that.neg };
		if (that.equals(Surreal.negOne)) { ^this.neg };

		// --- OPTIMIZATION 1: Integer * Integer ---
		if (this.isInteger and: { that.isInteger }) {
			^Surreal.integer(this.asInteger * that.asInteger);
		};

		// --- OPTIMIZATION 2: Surreal * Integer ---
		// First, figure out which one is the surreal (x) and which is the integer (y)
		if (that.isInteger) {
			x = this; y = that; // x is surreal, y is integer
		} {
			if (this.isInteger) {
				x = that; y = this; // x is surreal, y is integer
			} {
				x = nil; // No integer involved, will use fallback
			}
		};

		// If we found an integer to multiply by...
		if (x.notNil) {
			if (y.asInteger > 0) { // Case 1: Multiply by positive integer
				if (x >= Surreal.zero) { // Subcase 1a: x >= 0
					newL = x.l.collect({|xl| xl * y});
					newR = x.r.collect({|xr| xr * y});
				} { // Subcase 1b: x < 0
					newL = x.r.collect({|xr| xr * y});
					newR = x.l.collect({|xl| xl * y});
				}
			} { // Case 2: Multiply by negative integer
				// Use the identity: x*y = -(x*(-y))
				^((x * y.neg).neg);
			};
			^Surreal.prSimplifyAndNew(newL, newR);
		};


		// --- FALLBACK: General formula for Surreal * Surreal ---
		newL = Array.new; newR = Array.new;
		l.do { |xl| that.l.do { |yl| newL = newL.add((xl*that)+(this*yl)-(xl*yl)) } };
		r.do { |xr| that.r.do { |yr| newL = newL.add((xr*that)+(this*yr)-(xr*yr)) } };
		l.do { |xl| that.r.do { |yr| newR = newR.add((xl*that)+(this*yr)-(xl*yr)) } };
		r.do { |xr| that.l.do { |yl| newR = newR.add((xr*that)+(this*yl)-(xr*yl)) } };
		^Surreal.prSimplifyAndNew(newL, newR);
	}

	>= { |that|
		var cacheKey, result;
		that = that.asSurreal;
		if (this === that) { ^true };

		cacheKey = (this.hash.asString ++ "_" ++ that.hash.asString).asSymbol;
		result = comparisonCache.at(cacheKey);
		if (result.notNil) { ^result };

		result =
		(r.any({ |xr| that >= xr }).not) and: {
			(that.l.any({ |yl| yl >= this }).not)
		};

		comparisonCache.put(cacheKey, result);
		^result;
	}

	<= { |that| ^(that >= this) }

	>  { |that|
		that = that.asSurreal;
		// a > b is equivalent to (a >= b) AND (a != b), which is (a >= b) and not (a <= b)
		^((this >= that) and: { (this <= that).not })
	}

	<  { |that|
		that = that.asSurreal;
		// a < b is equivalent to (a <= b) AND (a != b), which is (a <= b) and not (a >= b)
		^((this <= that) and: { (this >= that).not })
	}

	equals { |that|
		var other = that.asSurreal;
		if (other.isNil) { ^false };
		// The definition of equality is mutual >= and <=
		^((this >= other) and: { (this <= other) });
	}

	// ==========================================================
	// ===== END OF FIX =========================================
	// ==========================================================

    != { |that| ^(this.equals(that).not) }

	== { |that| ^this.equals(that) }

    isZero { ^l.isEmpty and: r.isEmpty }
    isOne { ^this === Surreal.one }
    isInteger { ^integerReverseCache.at(this.hash).notNil }
    asInteger { ^integerReverseCache.at(this.hash) }
    generation { if(this.isZero) { ^0 }; ^(l++r).collect(_.generation).maxItem+1; }

    toFloat {
        var cached=floatCache.at(this.hash), value, leftBound, rightBound;
        if(cached.notNil) { ^cached };
        if(this.isZero) { value = 0.0 } {
            leftBound = if(l.isEmpty) { -inf } { l.collect(_.toFloat).maxItem };
            rightBound = if(r.isEmpty) { inf } { r.collect(_.toFloat).minItem };
            value = case
                { rightBound == inf } { leftBound + 1 }
                { leftBound == -inf } { rightBound - 1 }
                { (leftBound + rightBound) / 2 };
        };
        floatCache.put(this.hash, value);
        ^value;
    }

    asString { |maxDepth = 3| ^this.prBuildString(maxDepth, 0); }

    prBuildString { |maxDepth=3, currentDepth=0|
        var leftStr, rightStr;
        var intVal = this.asInteger;
        if(intVal.notNil) { ^intVal.asString };
        if(currentDepth >= maxDepth) { ^"{...}" };
        leftStr = l.collect { |x| x.prBuildString(maxDepth, currentDepth+1) }.join(", ");
        rightStr = r.collect { |x| x.prBuildString(maxDepth, currentDepth+1) }.join(", ");
        ^("{ " ++ leftStr ++ " | " ++ rightStr ++ " }");
    }

    printOn { |stream, maxDepth=3, currentDepth=0| stream << this.prBuildString(maxDepth, currentDepth); }
    asSurreal { ^this }
}

// ================= EXTENSIONS =================
+ Integer {
    isPowerOfTwo { ^(this > 0) and: { (this bitAnd: (this - 1)) == 0 }; }
    i { ^Surreal.integer(this) }
    asSurreal { ^Surreal.integer(this) }
}
+ Float {
    asSurreal {
        var num, den=1, err=1e-9, product;
        while {
            product = this * den;
            (product - product.round).abs > err and: { den < 65536 }
        } {
            den = den * 2;
        };
        num = (this * den).round.asInteger;
        ^Surreal.dyadic(num, den);
    }
}
+ Number { asSurreal { ^this.asFloat.asSurreal } }