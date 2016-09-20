.PHONY: diplomski.pdf all clean

all: diplomski.pdf

diplomski.pdf: main.tex
	latexmk -pdf -pdflatex="pdflatex -interaction=nonstopmode" -use-make main.tex

clean:
	latexmk -CA
