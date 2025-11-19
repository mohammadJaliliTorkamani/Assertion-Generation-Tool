package org.example.checker;

import org.example.AssertionFeatureMap;
import org.example.Parser;

public interface Checker {
    void check(Parser parser, AssertionFeatureMap assertionFeatureMap);
}
