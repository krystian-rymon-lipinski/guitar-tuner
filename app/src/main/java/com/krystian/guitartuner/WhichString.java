package com.krystian.guitartuner;

public enum WhichString {
    e1, // basic frequency 330 Hz - detected, 220 is also detected (also 110, but rarely)
    h,  // basic frequency 247 Hz - detected (110 is also rarely detected)
    g,  // basic frequency 196 Hz - detected, sometimes 392 is louder
    d,  // basic frequency 147 Hz - detected, sometimes 294 is louder
    A,  // basic frequency 110 Hz, 220 is sometimes louder
    E,  // basic frequency 82 Hz, but almost never detected; 164 as the loudest every time
    none
}
