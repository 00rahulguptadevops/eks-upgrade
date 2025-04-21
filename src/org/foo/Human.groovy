package org.foo

class Human {
    String name

    void sayName() {
        steps.sh "echo Hello, my name is ${name}"
    }
}
