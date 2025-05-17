  li $s0, 0
  li $s1, 1
func1:
  li $t2, 2
  move $t3, $zero
  li $t4, 3
  move $t5, $zero
  add $t6, $zero, $zero
  move $t7, $t6
  li $t8, 4
  add $t9, $zero, $t8
  move $t1, $t9
  li $t2, 5
  add $t3, $zero, $t2
  move $t4, $t3
  li $t5, 6
  add $t6, $zero, $t5
  move $t7, $t6
  li $t8, 7
  add $t1, $zero, $t8
  move $t9, $t1
  li $t2, 8
  add $t3, $zero, $t2
  move $t4, $t3
  li $t5, 10
  add $t7, $zero, $t5
  move $t6, $t7
  li $t8, 11
  add $t1, $zero, $t8
  move $t9, $t1
  li $t2, 12
  add $t3, $zero, $t2
  move $t4, $t3
  li $t5, 1
  li $v0, 0
  jr $ra
xat:
  move $v0, $s0
  jr $ra
