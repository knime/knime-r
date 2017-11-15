max: x1 + 2x2 - 4x3 -3x4;
x1 + x2 <= 5;
2x1 - x2 >= 0;
-x1 + 3x2 >= 0;
x3 + x4 >= .5;
x3 >= 1.1;
x3 <= 10;

x2abs >= x2;
x2abs >= -x2;

x4abs >= x4;
x4abs >= -x4;

x2abs + x4abs <= 1.5;

free x2, x4;
