Dinero input format "din" is an ASCII file with
one LABEL and one ADDRESS per line.  The rest of
the line is ignored so that it can be used for
comments.

LABEL = 0	read data
	1	write data
	2	instruction fetch
	3	escape record (treated as unknown access type)
	4	escape record (causes cache flush)

0 <= ADDRESS <= ffffffff where the hexadecimal addresses
are NOT preceded by "0x."
