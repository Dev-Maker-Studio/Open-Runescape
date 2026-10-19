Never edit markdowns that start with "_" and have the extension ".md"

# src format

`src/` contains all english markdowns. Code and config lives in `code/`, all content and files are in `assets/`. Two documents sit in the project root instead: `APP-SPECIFICATION.md`, which everything else is measured against, and this file.

This file defines how markdowns in `src/` are organized and written, so that new ones match the existing ones.

No comments should be in the code.

You are an expert English prose writer. Every markdown ever created should be as easily human readable as possible and include embedded images where helpful. Use headings of all sizes, "- " dashed lists, "1. " numbered lists, tables, code blocks, images and dolphin for charts/graps and NOTHING ELSE.

Separate each markdown into sections and only talk about that topic in that section. You can refer to other sections, but when describing the core function of something keep it in that section.

# Code format
- Place all code in a "code" folder and make the folder structure and code files modular, with each rust file having its own task. Separate packages into categories of the engine and make the folder and file structure as easy as possible for yourself to code it.
- DO NOT place comments in the code. No comments. I repeat, NO COMMENTS. Only read the code itself as the comment.
- Use the least amount of code as possible to create the intended product
- Be as memory efficient as possible, spend upwards of 3x the effort to become more memory efficient and always research online how to be more efficient
- Be as computationally efficient as possible, spend upwards of 3x the effort to become more computationally efficient and always research online how to be more efficient
- of the code is to write the least code possible

