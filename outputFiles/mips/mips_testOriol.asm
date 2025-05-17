  li $s0, 0
  li $s1, 1
func1:
  li $t2, 5
  slt $t3, $s0, $t2
  bne $t3, $zero, L0
  j L1
L0:
  li $t4, 1
  add $t5, $s0, $t4
  move $s0, $t5
  # Save context before calling func1
  sw $t0, -4($sp)
  sw $t1, -8($sp)
  sw $t2, -12($sp)
  sw $t3, -16($sp)
  sw $t4, -20($sp)
  sw $t5, -24($sp)
  sw $t6, -28($sp)
  sw $t7, -32($sp)
  sw $t8, -36($sp)
  sw $t9, -40($sp)
  sw $ra, -44($sp)
  sw $a0, -48($sp)
  addiu $sp, $sp, -48
  # Call function
  jal func1
  # Restore context after call
  lw $a0, 0($sp)
  lw $ra, 4($sp)
  lw $t9, 8($sp)
  lw $t8, 12($sp)
  lw $t7, 16($sp)
  lw $t6, 20($sp)
  lw $t5, 24($sp)
  lw $t4, 28($sp)
  lw $t3, 32($sp)
  lw $t2, 36($sp)
  lw $t1, 40($sp)
  lw $t0, 44($sp)
  addiu $sp, $sp, 48
  move $s1, $v0
L1:
  li $v0, 0
  jr $ra
xat:
  move $v0, $s0
  jr $ra
