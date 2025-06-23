// =================================================================
// Surreal.sc
// Surreal Numbers for SuperCollider
// =================================================================

Surreal {
    var <l, <r;
    var <hashValue; // Cache hash for efficient dictionary lookups

    classvar <>comparisonCache, <>floatCache, <>integerCache;

    *initClass {
        "Initializing Surreal Number Caches...".postln;
        comparisonCache = Dictionary.new;
        floatCache = Dictionary.new;
        integerCache = Dictionary.new;
    }

    *clearCaches {
        comparisonCache.clear;
        floatCache.clear;
        integerCache.clear;
        "Surreal Number Caches Cleared.".postln;
    }

    // --- Constructors ---
    *new { |left, right|
        ^super.new.init(left ? [], right ? [])
    }

    init { |left, right|
        l = left.asArray.sort;
        r = right.asArray.sort;

        // Validate surreal number construction
        this.prValidate;

        // Generate hash for caching
        hashValue = this.prGenerateHash;

        ^this;
    }

    // Private method to validate surreal number construction
    prValidate {
        l.do { |leftVal|
            r.do { |rightVal|
                if (leftVal >= rightVal) {
                    Error("Invalid Surreal: left element % >= right element %".format(leftVal, rightVal)).throw;
                };
            };
        };
    }

    // Fixed: Custom hash combination for SuperCollider compatibility
    prGenerateHash {
        var seed = 0;
        seed = this.prCustomHashCombine(seed, l.size.hash);
        l.do { |x| seed = this.prCustomHashCombine(seed, x.hash) };
        seed = this.prCustomHashCombine(seed, r.size.hash);
        r.do { |x| seed = this.prCustomHashCombine(seed, x.hash) };
        ^seed;
    }

    prCustomHashCombine { |a, b|
        ^(a * 31) + b;
    }

    hash { ^hashValue }

    *zero { ^this.integer(0) }

    *integer { |val=1|
        var result;

        val = val.asInteger;
        result = integerCache.at(val);
        if (result.notNil) { ^result };

        if (val == 0) {
            result = this.new([], []);
        } {
            if (val > 0) {
                result = this.new([this.integer(val - 1)], []);
            } {
                result = this.new([], [this.integer(val + 1)]);
            };
        };
        integerCache.put(val, result);
        ^result;
    }

    *dyadic { |num, den|
        var result;

        den = den.asInteger;
        if (den <= 0) {
            Error("Denominator must be positive").throw;
        };
        if (den.isPowerOfTwo.not) {
            Error("Denominator must be a power of two").throw;
        };

        num = num.asInteger;
        if (den == 1) { ^this.integer(num) };
        if (num.even) { ^this.dyadic(num / 2, den / 2) };
        ^this.new([this.dyadic(num - 1, den)], [this.dyadic(num + 1, den)]);
    }

    *fromFloat { |float|
        var cached, result;

        cached = floatCache.at(float);
        if (cached.notNil) { ^cached };

        if (float.isInteger) {
            result = this.integer(float);
        } {
            result = this.new([this.integer(float.floor)], [this.integer(float.ceil)]);
        };

        floatCache.put(float, result);
        ^result;
    }

    // --- Core Operations ---
    + { |that|
        var thisInt, thatInt, newL, newR;

        that = that.asSurreal;

        // Try to use cached arithmetic if both are simple integers
        thisInt = this.prGetCachedInteger;
        thatInt = that.prGetCachedInteger;
        if (thisInt.notNil and: thatInt.notNil) {
            ^Surreal.integer(thisInt + thatInt);
        };

        newL = l.collect(_ + that) ++ that.l.collect(this + _);
        newR = r.collect(_ + that) ++ that.r.collect(this + _);
        ^Surreal.new(newL, newR);
    }

    - { |that|
        var thisInt, thatInt;

        that = that.asSurreal;

        // Try to use cached arithmetic if both are simple integers
        thisInt = this.prGetCachedInteger;
        thatInt = that.prGetCachedInteger;
        if (thisInt.notNil and: thatInt.notNil) {
            ^Surreal.integer(thisInt - thatInt);
        };

        ^this + that.neg;
    }

    neg {
        var thisInt;

        thisInt = this.prGetCachedInteger;
        if (thisInt.notNil) {
            ^Surreal.integer(thisInt.neg);
        };
        ^Surreal.new(r.collect(_.neg), l.collect(_.neg));
    }

    * { |that|
    var thisInt, thatInt, newL, newR;

    that = that.asSurreal;

    // Try to use cached arithmetic if both are simple integers
    thisInt = this.prGetCachedInteger;
    thatInt = that.prGetCachedInteger;
    if (thisInt.notNil and: thatInt.notNil) {
        ^Surreal.integer(thisInt * thatInt);
    };

    // Special cases for zero and one
    if (this.prIsZero) { ^Surreal.zero };  // Fixed: return zero, not this
    if (that.prIsZero) { ^Surreal.zero };  // Fixed: return zero, not that
    if (this.prIsOne) { ^that };
    if (that.prIsOne) { ^this };

    newL = Array.new;
    newR = Array.new;

    // Conway's multiplication formula:
    // xy = {xL*y + x*yL - xL*yL, xR*y + x*yR - xR*yR | xL*y + x*yR - xL*yR, xR*y + x*yL - xR*yL}

    // Left set: xL*y + x*yL - xL*yL (from left-left combinations)
    l.do { |xl|
        that.l.do { |yl|
            newL = newL.add((xl * that) + (this * yl) - (xl * yl));
        };
    };

    // Left set: xR*y + x*yR - xR*yR (from right-right combinations)
    r.do { |xr|
        that.r.do { |yr|
            newL = newL.add((xr * that) + (this * yr) - (xr * yr));
        };
    };

    // Right set: xL*y + x*yR - xL*yR (from left-right combinations)
    l.do { |xl|
        that.r.do { |yr|
            newR = newR.add((xl * that) + (this * yr) - (xl * yr));
        };
    };

    // Right set: xR*y + x*yL - xR*yL (from right-left combinations)
    r.do { |xr|
        that.l.do { |yl|
            newR = newR.add((xr * that) + (this * yl) - (xr * yl));
        };
    };

    ^Surreal.new(newL, newR);
	}

    // Helper methods for optimization
    prGetCachedInteger {
        integerCache.keysValuesDo { |intVal, sur|
            if (sur === this) { ^intVal };
        };
        ^nil;
    }

    prIsZero {
        ^l.isEmpty and: r.isEmpty;
    }

    prIsOne {
        var cachedInt = this.prGetCachedInteger;
        ^cachedInt == 1;
    }

    // --- Comparison ---
    >= { |that|
        var thisInt, thatInt, cacheKey, result, geqResult;

        that = that.asSurreal;

        // Quick integer comparison optimization
        thisInt = this.prGetCachedInteger;
        thatInt = that.prGetCachedInteger;
        if (thisInt.notNil and: thatInt.notNil) {
            ^thisInt >= thatInt;
        };

        cacheKey = [this.hash, that.hash, ">="];
        result = comparisonCache.at(cacheKey);
        if (result.notNil) { ^result };

        // Base definition: x >= y iff no xR <= y and no yL >= x
        geqResult = true;

        // Check if any right element of this is <= that
        r.do { |xR|
            if (xR.prDirectLessEqual(that)) {
                geqResult = false;
            };
        };

        // Check if any left element of that is >= this
        if (geqResult) {
            that.l.do { |yL|
                if (yL.prDirectGreaterEqual(this)) {
                    geqResult = false;
                };
            };
        };

        comparisonCache.put(cacheKey, geqResult);
        ^geqResult;
    }

    // Private helper methods to avoid infinite recursion
    prDirectLessEqual { |that|
        var thisInt, thatInt;

        // Use basic comparison for integers to avoid recursion
        thisInt = this.prGetCachedInteger;
        thatInt = that.prGetCachedInteger;
        if (thisInt.notNil and: thatInt.notNil) {
            ^thisInt <= thatInt;
        };

        // For non-integers, use the >= definition in reverse
        ^that.prDirectGreaterEqual(this);
    }

    prDirectGreaterEqual { |that|
        var thisInt, thatInt, result;

        // Use basic comparison for integers to avoid recursion
        thisInt = this.prGetCachedInteger;
        thatInt = that.prGetCachedInteger;
        if (thisInt.notNil and: thatInt.notNil) {
            ^thisInt >= thatInt;
        };

        // Simplified recursive definition check
        result = true;

        // Check if any right element of this is <= that (base case)
        this.r.do { |xR|
            var xRInt, thatInt;
            xRInt = xR.prGetCachedInteger;
            thatInt = that.prGetCachedInteger;
            if (xRInt.notNil and: thatInt.notNil) {
                if (xRInt <= thatInt) { result = false };
            };
        };

        // Check if any left element of that is >= this (base case)
        if (result) {
            that.l.do { |yL|
                var yLInt, thisInt;
                yLInt = yL.prGetCachedInteger;
                thisInt = this.prGetCachedInteger;
                if (yLInt.notNil and: thisInt.notNil) {
                    if (yLInt >= thisInt) { result = false };
                };
            };
        };

        ^result;
    }

    < { |that| ^(this >= that).not }
    <= { |that| ^that >= this }
    > { |that| ^(this <= that).not }
    == { |that| ^(this >= that) && (this <= that) }
    != { |that| ^(this == that).not }

    // --- Generative & Musical Methods ---
    s { |that|
        that = that.asSurreal;
        if (this >= that) {
            Error("Cannot create a Surreal number where left >= right").throw;
        };
        ^Surreal.new([this], [that]);
    }

    generation {
        var maxGen;

        maxGen = -1;
        if (l.isEmpty and: r.isEmpty) { ^0 };
        (l ++ r).do { |s|
            maxGen = maxGen.max(s.generation);
        };
        ^maxGen + 1;
    }

    toFloat { |min=0, max=1, unscaled=false|
        var cacheKey, value, leftBound, rightBound;

        cacheKey = [this.hash, "toFloat"];
        value = floatCache.at(cacheKey);

        if (value.isNil) {
            leftBound = if (l.isEmpty) { -inf } {
                l.collect(_.toFloat(unscaled: true)).maxItem
            };
            rightBound = if (r.isEmpty) { inf } {
                r.collect(_.toFloat(unscaled: true)).minItem
            };

            value = case
                { leftBound == -inf && rightBound == inf } { 0 }
                { rightBound == inf } { leftBound + 1 }
                { leftBound == -inf } { rightBound - 1 }
                { (leftBound + rightBound) / 2 };

            floatCache.put(cacheKey, value);
        };

        if (unscaled) { ^value };
        ^value.linlin(-2, 2, min, max);
    }

    // --- UTILITY METHODS ---
    printOn { |stream, maxDepth=3, currentDepth=0|
    var val;
    var floatVal, num, den, foundFraction;
    var epsilon = 1e-6;

    // Check if this is a cached integer
    integerCache.keysValuesDo { |intVal, sur|
        if (sur === this) {
            val = intVal;
        };
    };

    if (val.notNil) {
        stream << val.asString;
    } {
        if (currentDepth >= maxDepth) {
            stream << "{...}";
            ^this;
        };

        floatVal = this.toFloat(unscaled: true);

        // Try to represent as simple fraction
        foundFraction = false;
        den = 1;
        while { den <= 64 } {  // Check denominators up to 64
            num = (floatVal * den).round(epsilon);
            if ((num - num.round).abs < epsilon) {
                num = num.round.asInteger;
                foundFraction = true;

                if (num == 0) {
                    stream << "0";
                } {
                    // Simplify fraction
                    var gcd = num.gcd(den);
                    var simpNum = num div: gcd;
                    var simpDen = den div: gcd;

                    // ALWAYS show as improper fraction
                    stream << simpNum << "/" << simpDen;
                };
                ^this;
            };
            den = den + 1;
        };

        // Fallback to structural representation
        stream << "{";
        l.do { |x, i|
            x.printOn(stream, maxDepth, currentDepth + 1);
            if (i < (l.size - 1)) { stream << ", " };
        };
        stream << " | ";
        r.do { |x, i|
            x.printOn(stream, maxDepth, currentDepth + 1);
            if (i < (r.size - 1)) { stream << ", " };
        };
        stream << "}";
    };
	}

    prIsDyadic {
        // Simple heuristic: if it has exactly one element on each side and they are integers
        ^(l.size <= 2) and: (r.size <= 2) and: {
            l.every { |x| x.prGetCachedInteger.notNil } and: {
                r.every { |x| x.prGetCachedInteger.notNil }
            }
        };
    }

    asString {
        var stream = CollStream.new;
        this.printOn(stream);
        ^stream.collection;
    }

    asSurreal { ^this }
}

// =================================================================
// Extensions for convenience (MUST be outside the class)
// =================================================================

+ Integer {
    isPowerOfTwo {
        ^(this > 0) and: { (this & (this - 1)) == 0 };
    }

    i {
        ^Surreal.integer(this);
    }

    asSurreal {
        ^Surreal.integer(this);
    }
}

+ Float {
    asSurreal {
        ^Surreal.fromFloat(this);
    }
}

+ Number {
    asSurreal {
        ^Surreal.fromFloat(this.asFloat);
    }
}