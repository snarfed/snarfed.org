#!/usr/bin/python
#
# thx_unittest.py
# Copyright 2004 Ryan Barrett <thx.py@ryanb.org>

"""
Unit tests for thx.py.
"""

import os
import sys
import string
import unittest
import thx


CIPHERFILE = ['0 _',
              '1 a',
              '2 b',
              '3 c',
              '4 d',
              '5 e',
              '6 f',
              '7 g',
              '8 h',
              '9 i',
              '10 j',
              '11 k',
              '12 l',
              '13 m',
              '14 n',
              '15 o',
              '16 p',
              '17 q',
              '18 r',
              '19 s',
              '20 t',
              '21 u',
              '22 v',
              '23 w',
              '24 x',
              '25 y',
              '26 z', ]

ALPHABET = dict(zip(range(0, 27), '_' + string.ascii_lowercase))
PADDED = dict(zip(range(0, 32), '_' + string.ascii_lowercase + ' ' * 5))

PLAINTEXT = 'subjecte_prefix_blz'
CIPHERTEXT = 'WVVWWWVWVWVVVWVVWVWVVVWVWVVVWWWVWVVVVWVWVVVVVWVVVVWVVWVVVWVWVVWWVVWVVWWWVVVVVVVVVVVWVVWWVVWWVWV'

class thxTest(unittest.TestCase):

  def test_read_cipherfile(self):
    self.assertEqual(ALPHABET, thx.read_cipherfile(CIPHERFILE))

  def test_encrypt(self):
    self.assertEqual(CIPHERTEXT, thx.encrypt(PLAINTEXT, ALPHABET))
    self.assertEqual(PLAINTEXT, thx.decrypt(CIPHERTEXT, ALPHABET))

    # not meaningful after the above tests, but still fun to check
    decrypted = thx.decrypt(thx.encrypt(PLAINTEXT, ALPHABET), ALPHABET)
    self.assertEqual(PLAINTEXT, decrypted)

  def test_pad_alphabet(self):
    self.assertEqual(PADDED, thx.pad_alphabet(ALPHABET))

  def test_itoa2(self):
    for n in 123, 123411, 4569, 12341234, 2456:
      self.assertEqual(n, int(thx.itoa2(n), 2))

  def test_subseq(self):
    assert thx.subset([], [])
    assert not thx.subset([], [1])
    assert thx.subset([1], [])
    assert thx.subset([1], [1])
    assert not thx.subset([1], [2])
    assert not thx.subset([1], [1, 2])
    assert thx.subset([1, 2], [1])
    assert not thx.subset([1, 2], [1, 3])

if __name__ == '__main__':
  unittest.main()
