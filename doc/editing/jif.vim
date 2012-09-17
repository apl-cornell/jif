" Jif syntax file

set ts=4
set sw=4

syn match op        "->"
syn match op        "<-"
syn keyword javaMethodDecl where class principal public equiv void if return while actsfor private final new
syn region commentRegion matchgroup=comment start="//" end ="$"
syn region commentRegion matchgroup=comment start="/\*" end="\*/"
syn keyword StorageClass principal label actsfor

if !exists("main_syntax")
  let main_syntax='jif'
endif

syn clear javaLabelRegion
hi link op StorageClass
hi link javaMethodDecl Statement
hi link commentRegion Comment
