  li $s0, 0
  li $s1, 1
func1:
L0:
  li $t0, 5
  slt $t1, $s0, $t0
  bne $t1, $zero, L1
  j L2
L1:
  li $t3, 1
  add $t4, $s0, $t3
  move $s0, $t4
  add $t5, $s1, $t0
  move $s1, $t5
  j L0
L2:
  li $v0, 69
  jr $ra
xat:
  li $t7, 0
  # Save context before calling func1
  addiu $sp, $sp, -68
  sw $t0, 0($sp)
  sw $t1, 4($sp)
  sw $t2, 8($sp)
  sw $t3, 12($sp)
  sw $t4, 16($sp)
  sw $t5, 20($sp)
  sw $t6, 24($sp)
  sw $t7, 28($sp)
  sw $t8, 32($sp)
  sw $t9, 36($sp)
  sw $s0, 40($sp)
  sw $s1, 44($sp)
  sw $s2, 48($sp)
  sw $s3, 52($sp)
  sw $s4, 56($sp)
  sw $ra, 60($sp)
  sw $a0, 64($sp)
  # Call function
  jal func1
  # Restore context after call
  lw $t0, 0($sp)
  lw $t1, 4($sp)
  lw $t2, 8($sp)
  lw $t3, 12($sp)
  lw $t4, 16($sp)
  lw $t5, 20($sp)
  lw $t6, 24($sp)
  lw $t7, 28($sp)
  lw $t8, 32($sp)
  lw $t9, 36($sp)
  lw $s0, 40($sp)
  lw $s1, 44($sp)
  lw $s2, 48($sp)
  lw $s3, 52($sp)
  lw $s4, 56($sp)
  lw $ra, 60($sp)
  lw $a0, 64($sp)
  addiu $sp, $sp, 68
  move $t7, $v0
  li $v0, 0
  jr $ra
