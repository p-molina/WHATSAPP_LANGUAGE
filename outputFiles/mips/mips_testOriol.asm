  li $s0, 10
  li $s1, 0
  li $s2, 0
  li $s3, 0
  li $s4, 0
fibonacci:
  li $t0, 2
  slt $t1, $s0, $t0
  bne $t1, $zero, L0
  li $t3, 1
  sub $t4, $s0, $t3
  move $s0, $t4
  # Save context before calling fibonacci
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
  jal fibonacci
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
  move $s2, $v0
  sub $t6, $s0, $t3
  move $s0, $t6
  # Save context before calling fibonacci
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
  jal fibonacci
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
  move $s3, $v0
  add $t8, $s2, $s3
  move $s1, $t8
  j L1
L0:
  move $s1, $s0
L1:
  move $v0, $s1
  jr $ra
xat:
  # Save context before calling fibonacci
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
  jal fibonacci
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
