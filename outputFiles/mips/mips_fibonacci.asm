.text
.globl main
main:
li $t0, 10
fibonacci:
li $t1, 2
bne $t2, $zero, L0
li $t3, 1
j L1
L0:
L1:
move $v0, $t9
jr $ra
xat:
move $v0, $t0
jr $ra
