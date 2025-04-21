package org.foo

class Human implements Serializable {
    def steps
    String name

    Human(steps, name) {
        this.steps = steps
        this.name = name
    }

    void sayName() {
        steps.sh "echo Hello, my name is ${name}"
    }
}

