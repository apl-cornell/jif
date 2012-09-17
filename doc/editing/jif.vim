" Jif syntax file

set ts=4
set sw=4

syn match op        "->"
syn match op        "<-"
syn keyword javaMethodDecl where class public void actsfor private final new implements
syn keyword Statement  if return while try catch
syn region commentRegion matchgroup=comment start="//" end ="$"
syn region commentRegion matchgroup=comment start="/\*" end="\*/"
syn keyword StorageClass principal label actsfor equiv provider

if !exists("main_syntax")
  let main_syntax='jif'
endif

hi link op StorageClass
hi link javaMethodDecl Statement
hi link commentRegion Comment
