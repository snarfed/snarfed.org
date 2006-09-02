#!/usr/bin/python2
#
# thx.py
# Copyright 2004 Ryan Barrett <thx.py@ryanb.org>
#
# See docstring for usage details.

USAGE = """Usage: thx [-d|-e] CIPHERFILE

Decrypts and encrypts according to the substitution ciphers from the Subcity
Central (aka THX-1138) ARG. The message is read from stdin. The cipher file is
a filemapping numbers to letters, one per line, like so:

6 a
23 b
5 c
...

Options:
  -d        decrypt message (default)
  -e        encrypt message
  -b bits   number of bits per letter; defaults to 5

For more information about the Subcity Central ARG, see:
  http://forums.unfiction.com/forums/viewforum?f=88
"""

import sys
import string
import getopt

# constants
CODE_CHARS = {
    'V': '0',
    'W': '1', }

# command-line options
DECRYPT   = True
BITS_PER_LETTER = 5


def main(filename):
    lines = open(filename).readlines()
    alphabet = pad_alphabet(read_cipherfile(lines))

    for line in sys.stdin.readlines():
        line = line.strip()
        if DECRYPT:
            print decrypt(line, alphabet)
        else:
            print encrypt(line, alphabet)


def decrypt(ciphertext, alphabet):
    """ Decrypts the given ciphertext using the given alphabet. Returns the
    decrypted plaintext.
    """
    # convert to binary
    for fromchar, tochar in CODE_CHARS.items():
        ciphertext = ciphertext.replace(fromchar, tochar)

    # split every BITS_PER_LETTER chars
    assert len(ciphertext) % BITS_PER_LETTER == 0
    split = [ciphertext[index:index + BITS_PER_LETTER]
             for index in range(0, len(ciphertext), BITS_PER_LETTER)]

    # decode!
    numbers = [int(binary, 2) for binary in split]
    letters = [alphabet[num] for num in numbers]
    return string.join(letters, '')


def encrypt(plaintext, alphabet):
    """ Encrypts the given plaintext using the given alphabet. Returns the
    encrypted ciphertext.
    """
    reverse_alphabet = dict(zip(alphabet.values(), alphabet.keys()))
#    assert len(alphabet) == len(reverse_alphabet)

    # encode
    split = list(plaintext)   # list of the letters
    numbers = [reverse_alphabet[letter] for letter in split]

    # convert to binary
    binary = [itoa2(num) for num in numbers]
    padded = ['0' * (BITS_PER_LETTER - len(num)) + num for num in binary]
    joined = string.join(padded, '')

    # convert to Vs and Ws
    for tochar, fromchar in CODE_CHARS.items():
        joined = joined.replace(fromchar, tochar)
    return joined

def pad_alphabet(alphabet):
    """ Pads the alphabet; maps all unmapped numbers to space.
    """
    padded = dict(alphabet)
    
    for i in range(0, 2 ** BITS_PER_LETTER):
        if i not in alphabet:
            padded[i] = ' '

    return padded


def read_cipherfile(lines):
    """ Reads a cipher file and returns a mapping from [0,26] to [a,z].
    """
    stripped = [line.strip() for line in lines if line.strip()]
    split = [string.split(line, ' ', 1) for line in stripped]
    sanitized = [(int(num), letter.strip().lower()) for num, letter in split]
    map = dict(sanitized)

#    assert len(map) >= 26  # letters and space
#    assert subset(map.keys(), range(0, 27))
#    assert subset(map.values(), string.ascii_lowercase)

    return map


def itoa2(n):
    """ Returns the base 2 representation of a number, as a string. Taken from:
    http://mail.python.org/pipermail/python-list/2001-November/074834.html
    """
    if n < 1:
        return ''
    else:
        return itoa2(n / 2) + str(n & 1)


def subset(seq, subseq):
    """ Returns true if all of the items in subseq are also in seq, false
    otherwise.
    """
    contained = [x for x in seq if x in subseq]
    return len(contained) == len(subseq)  # use len in case subseq is a string


def parse_args(args):
    """ Parse command-line args. (See doc comment.)
    """
    global DECRYPT, BITS_PER_LETTER

    try:
        options, args = getopt.getopt(args, 'hdeb:', '--help')
    except getopt.GetoptError:
        type, value, traceback = sys.exc_info()
        print >> sys.stderr, value.msg
        usage()
        sys.exit(2)

    if '-d' in options and '-e' in options:
        print >> sys.stderr, "Can't specify both -d and -e."
        usage()
        sys.exit(2)

    for option, arg in options:
        if option in ('-h', '--help'):
            usage()
            sys.exit(2)
        elif option == '-d':
            DECRYPT = True
        elif option == '-e':
            DECRYPT = False
        elif option == '-b':
            BITS_PER_LETTER = int(arg)
            assert BITS_PER_LETTER >= 5
    # end for option, arg

    if not args:
        usage()
        sys.exit(2)

    assert len(args) == 1
    return args[0]

def usage():
    print USAGE

if __name__ == '__main__':
    arg = parse_args(sys.argv[1:])
    main(arg)
