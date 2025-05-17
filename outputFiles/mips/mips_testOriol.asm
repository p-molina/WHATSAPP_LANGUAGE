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
  jal fibonacci
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
  move $s2, $v0
  sub $t6, $s0, $t3
  move $s0, $t6
  # Save context before calling fibonacci
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
  jal fibonacci
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
  jal fibonacci
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
  move $s4, $v0
  move $v0, $s4
  jr $ra
