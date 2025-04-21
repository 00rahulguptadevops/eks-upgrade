package org.foo

class Human {
    String name

    void sayName(){
        sh 'echo ${name}'
    }
}
