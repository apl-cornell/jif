JIFC = ../bin/jifc


GOOD_TEST_CASES = \
	Account.jif \
	ActsFor1.jif \
	ActsFor2.jif \
	ActsFor3.jif \
	ActsFor4.jif \
	ActsFor5.jif \
	A.jif \
	ArgLabelSubst2.jif \
	ArgLabelSubst.jif \
	Array2.jif \
	Array.jif \
	Binary.jif \
	Call1.jif \
	cat.jif \
	clientEmptyList.jif \
	clientList.jif \
	clientPairList.jif \
	Complex.jif \
	CondExpr.jif \
	Constraints1.jif \
	Constraints3.jif \
	Constraints5.jif \
	ConstructorTest.jif \
	Default1.jif \
	Deref2.jif \
	Deref.jif \
	Dyn1.jif \
	Dyn2.jif \
	Dyn4.jif \
	Dyn5.jif \
	Dyn6.jif \
	EmptyList2.jif \
	List.jif \
	EmptyList.jif \
	Exc2.jif \
	Exc.jif \
	For1.jif \
	Heapsort.jif \
	Hello.jif \
	If1.jif \
	Implicit2.jif \
	Implicit.jif \
	InterfaceTest.jif \
	IntPair.jif \
	List2.jif \
	ListTest.jif \
	Main2.jif \
	MyInteger.jif \
	NewLabel.jif \
	NullExcept1.jif \
	NullExcept2.jif \
	NullExcept3.jif \
	NullExcept4.jif \
	NullExcept5.jif \
	NullExcept6.jif \
	NullExcept7.jif \
	NullExcept8.jif \
	NullExcept9.jif \
	PairList2.jif \
	PairList.jif \
	Params10.jif \
	Params11.jif \
	Params12.jif \
	Params1.jif \
	Params1Test.jif \
	Params1TestTest.jif \
	Params2.jif \
	Params3.jif \
	Params4.jif \
	Params5.jif \
	Params6.jif \
	Params7.jif \
	Params8.jif \
	Params9.jif \
	ParamsActsFor.jif \
	Password2.jif \
	Password.jif \
	Poly.jif \
	Simple2.jif \
	Simple3.jif \
	Simple4.jif \
	Simple.jif \
	StringTester.jif \
	SubtypeChecker2.jif \
	Super2.jif \
	Super5.jif \
	This1.jif \
	This2.jif \
	Vec.jif \
	While.jif 

# These test cases are meant to fail
BAD_TEST_CASES = \
	ArrayLeak.jif \
	ArrayLeak2.jif \
	BadNullExcept1.jif \
	BadNullExcept2.jif \
	BadNullExcept3.jif \
	BadNullExcept4.jif \
	BadNullExcept5.jif \
	BadNullExcept6.jif \
	BadNullExcept7.jif \
	Constraints7.jif \
	HiRefLowField.jif \
	InvariantClass.jif \
	Main1.jif \
	Override.jif \
	Params13.jif \
	Params14.jif \
	SubtypeChecker1.jif \
	SubtypeChecker3.jif \
	SubtypeChecker4.jif \
	Super1.jif \
	Super3.jif \
	Super4.jif \
	Super6.jif \
	ToString.jif

# Not so sure about these test cases... could be bugs. Need more
# investigation
UNKNOWN_TEST_CASES = \
	Constraints2.jif \
	Constraints4.jif \
	Constraints6.jif \
	Declassify.jif \
	FileTester.jif \
	Protected.jif \
	SwitchLabel.jif \
	SwitchLabel2.jif \
	SwitchLabel3.jif \
	SwitchLabel4.jif \
	User1.jif

CLASSES = $(patsubst %.jif, %.class, $(GOOD_TEST_CASES))


JIF_PATHS = -d . -sourcepath .

%.class: %.jif
	$(JIFC) $(JIF_PATHS) -nooutput $<

test: $(CLASSES)

retest: cleantest test

cleantest:
	rm -f *.class

all: 
	$(subdirs)

clean clobber:
	rm -f *.class
	$(subdirs)

DEMOS = \
	Hello.jif \
	cat.jif \

