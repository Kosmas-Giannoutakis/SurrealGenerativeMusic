// =================================================================
// Surreal Number Validation Tests (SuperCollider Compatible)
// =================================================================

// Test 1: Basic Construction and Identity
(
var zero, one, negOne;
"=== TEST 1: Basic Construction ===".postln;
zero = Surreal.zero;
one = Surreal.one;
negOne = Surreal.negOne;

zero.postln;        // Should print: 0
one.postln;         // Should print: 1
negOne.postln;      // Should print: -1

// Test identity caching
("Zero identity check: " ++ (Surreal.zero === Surreal.zero)).postln;  // Should be true
("One identity check: " ++ (Surreal.one === Surreal.one)).postln;     // Should be true
)

// Test 2: Integer Construction
(
var nums;
"=== TEST 2: Integer Construction ===".postln;
nums = (-3..3).collect { |i| Surreal.integer(i) };
nums.do { |n| n.postln };  // Should print: -3, -2, -1, 0, 1, 2, 3

// Test integer recognition
nums.do { |n|
    (n.asString ++ " isInteger: " ++ n.isInteger ++ " asInteger: " ++ n.asInteger).postln;
};
)

// Test 3: Dyadic Fractions
(
var half, quarter, threeHalves;
"=== TEST 3: Dyadic Fractions ===".postln;
half = Surreal.dyadic(1, 2);        // 1/2
quarter = Surreal.dyadic(1, 4);     // 1/4
threeHalves = Surreal.dyadic(3, 2); // 3/2

("1/2: " ++ half.asString).postln;
("1/4: " ++ quarter.asString).postln;
("3/2: " ++ threeHalves.asString).postln;

// Test their float approximations
("1/2 as float: " ++ half.toFloat).postln;        // Should be 0.5
("1/4 as float: " ++ quarter.toFloat).postln;     // Should be 0.25
("3/2 as float: " ++ threeHalves.toFloat).postln; // Should be 1.5
)

// Test 4: Basic Arithmetic
(
var a, b, sum, diff, prod, half, result;
"=== TEST 4: Basic Arithmetic ===".postln;
a = Surreal.integer(2);
b = Surreal.integer(3);
sum = a + b;
diff = a - b;
prod = a * b;

("2 + 3 = " ++ sum.asString ++ " (" ++ sum.asInteger ++ ")").postln;   // Should be 5
("2 - 3 = " ++ diff.asString ++ " (" ++ diff.asInteger ++ ")").postln; // Should be -1
("2 * 3 = " ++ prod.asString ++ " (" ++ prod.asInteger ++ ")").postln; // Should be 6

// Test with fractions
half = Surreal.dyadic(1, 2);
result = a + half;  // 2 + 1/2 = 5/2
("2 + 1/2 = " ++ result.asString ++ " ≈ " ++ result.toFloat).postln; // Should be 2.5
)

// Test 5: Comparisons
(
var zero, one, two, half;
"=== TEST 5: Comparisons ===".postln;
zero = Surreal.zero;
one = Surreal.one;
two = Surreal.integer(2);
half = Surreal.dyadic(1, 2);

("0 < 1: " ++ (zero < one)).postln;      // true
("1 < 2: " ++ (one < two)).postln;       // true
("1/2 < 1: " ++ (half < one)).postln;    // true
("0 < 1/2: " ++ (zero < half)).postln;   // true
("1 > 1/2: " ++ (one > half)).postln;    // true
("2 == 2: " ++ (two == two)).postln;     // true
)

// Test 6: Negation
(
var nums;
"=== TEST 6: Negation ===".postln;
nums = [0, 1, -1, 2, -3].collect { |n| Surreal.integer(n) };
nums.do { |n|
    var negated = n.neg;
    (n.asString ++ " negated is " ++ negated.asString).postln;
};

// Test double negation
nums.do { |x|
    var doubleNeg = x.neg.neg;
    ("Double negation: " ++ x.asString ++ " -> " ++ doubleNeg.asString ++ " equal? " ++ (x == doubleNeg)).postln;
};
)

// Test 7: Generation Numbers
(
var zero, one, negOne, half, quarter;
"=== TEST 7: Generation Numbers ===".postln;
zero = Surreal.zero;
one = Surreal.one;
negOne = Surreal.negOne;
half = Surreal.dyadic(1, 2);
quarter = Surreal.dyadic(1, 4);

(zero.asString ++ " generation: " ++ zero.generation).postln;     // 0
(one.asString ++ " generation: " ++ one.generation).postln;       // 1
(negOne.asString ++ " generation: " ++ negOne.generation).postln; // 1
(half.asString ++ " generation: " ++ half.generation).postln;     // 2
(quarter.asString ++ " generation: " ++ quarter.generation).postln; // 3
)

// Test 8: Float Conversion
(
var testFloats;
"=== TEST 8: Float Conversion ===".postln;
testFloats = [0.5, 0.25, 0.75, 1.5, -0.5];
testFloats.do { |f|
    var surreal, backToFloat;
    surreal = f.asSurreal;
    backToFloat = surreal.toFloat;
    (f.asString ++ " -> " ++ surreal.asString ++ " -> " ++ backToFloat ++ " match: " ++ ((f - backToFloat).abs < 0.001)).postln;
};
)

// Test 9: Complex Expressions
(
var a, b, c, result1, result2;
"=== TEST 9: Complex Expressions ===".postln;
a = Surreal.integer(3);
b = Surreal.dyadic(1, 2);  // 1/2
c = Surreal.integer(-1);

// Test: (3 + 1/2) * (-1) = -7/2
result1 = (a + b) * c;
("(3 + 1/2) * (-1) = " ++ result1.asString ++ " ≈ " ++ result1.toFloat).postln;

// Test: 3 - (1/2 + 1) = 3/2
result2 = a - (b + Surreal.one);
("3 - (1/2 + 1) = " ++ result2.asString ++ " ≈ " ++ result2.toFloat).postln;
)

// Test 10: Edge Cases
(
var zero, five, one;
"=== TEST 10: Edge Cases ===".postln;

// Test zero operations
zero = Surreal.zero;
five = Surreal.integer(5);
(zero.asString ++ " + " ++ five.asString ++ " = " ++ (zero + five).asString).postln;
(zero.asString ++ " * " ++ five.asString ++ " = " ++ (zero * five).asString).postln;

// Test one operations
one = Surreal.one;
(one.asString ++ " * " ++ five.asString ++ " = " ++ (one * five).asString).postln;

// Test self-operations
(five.asString ++ " - " ++ five.asString ++ " = " ++ (five - five).asString).postln;
)

// Test 11: Ordering
(
var numbers;
"=== TEST 11: Ordering ===".postln;
numbers = [
    Surreal.integer(-2),
    Surreal.integer(-1),
    Surreal.dyadic(-1, 2),  // -1/2
    Surreal.zero,
    Surreal.dyadic(1, 4),   // 1/4
    Surreal.dyadic(1, 2),   // 1/2
    Surreal.one,
    Surreal.dyadic(3, 2),   // 3/2
    Surreal.integer(2)
];

"Original order:".postln;
numbers.do { |n| ("  " ++ n.asString).postln };

"Checking if properly ordered (ascending):".postln;
(numbers.size - 1).do { |i|
    var current = numbers[i];
    var next = numbers[i + 1];
    var isCorrect = current <= next;
    ("  " ++ current.asString ++ " <= " ++ next.asString ++ ": " ++ isCorrect).postln;
};
)

// Test 12: Quick Performance Check
(
var startTime, endTime;
"=== TEST 12: Performance ===".postln;
startTime = Main.elapsedTime;

// Create same numbers multiple times - should use cache
50.do {
    var a = Surreal.integer(42);
    var b = Surreal.dyadic(3, 4);
    var c = a + b;
    var d = c.neg;
};

endTime = Main.elapsedTime;
("Cache test completed in " ++ (endTime - startTime) ++ " seconds").postln;

// Check cache contents
("Integer cache size: " ++ Surreal.integerCache.size).postln;
("Existing surreals cache size: " ++ Surreal.existingSurreals.size).postln;
)

"=== ALL TESTS COMPLETED ===".postln;
"Check output above for any unexpected results.".postln;