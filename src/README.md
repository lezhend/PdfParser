#A PDF Parser tool based on PdfBox and Tabula.

Give a PDF file and output path, extract text and table and save as .txt file.
Result is pure text and structured table content in sequence. Table content will be 
wrapped in ```<table></table>``` so that you can easily filter it.
```
Usage: "arg1: <input_file>; arg2: <output_directory>; arg3(optional): <gate_value>"
```
- 'input_file' is the full path to PDF file to be parsed.
- 'output_directory' is the target directory to save result .txt file.
- 'gate_value' is the degree to which we trust Tabula. Because Tabula may sometimes
fails to correctly detect table in PDF. a valid gate should be from 0 to 1. Leave it
default to be 0.75.
- concord to Alibaba Java coding style.
