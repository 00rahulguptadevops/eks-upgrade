package org.foo

class Human {
    String name
    def steps
    void sayName() {
        steps.sh "echo Hello, my name is ${name}"
    }
}
