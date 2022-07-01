package com.github.stevenlmcgraw.intellijcodegenerator2.config

const val AVAILABLE_MEMBERS = "availableMembers"

const val BODY = "body"

const val CARET = "caret"

const val CHOOSE_DIRECTORY_TO_EXPORT = "Choose Directory to Export"

const val CHOOSE_FILE_TO_IMPORT = "Choose File to Import"

const val CLASS = "class"

const val CLASS_NAME_VM = "\$class0.qualifiedName"

const val CLASS_NAME_VM_TEST = "\${class0.qualifiedName}Test"

const val CLASS_SELECTION = "class-selection"

const val CLASS_ZERO = "class0"

const val CODE_MAKER_MENU_ACTION_PREFIX = "CodeGenerator2.Menu.Action."

const val CODE_TEMPLATES = "Code Templates"

const val DEFAULT_ENCODING = "UTF-8"

const val DEFAULT_INCLUDE_LOCATION = "/template/default-include.vm"

const val DEFAULT_XML_EXPORT_PATH = "template.xml"

const val DESCRIPTION = "description"

const val DOT_JAVA_SUFFIX = ".java"

const val EMPTY_STRING = ""

const val FILE_NAME = "filename"

const val FILE_NAME_PATTERN = ".*\\.java$"

const val FORWARD_SLASH = "/"

const val IMPORT = "Import"

const val IMPORT_ERROR = "Import Error"

const val IMPORT_FAILED = "Import failed!"

const val IMPORT_FINISHED = "Import finished!"

const val INCLUDES = "Includes"

const val LINE_SEPARATOR = "line.separator"

const val MEMBER = "member"

const val MEMBER_SELECTION = "member-selection"

const val MEMBER_SELECTION_CONFIG_DEFAULT_TEMPLATE = ("## set `availableMembers` to provide the members to select\n"
        + "## set `selectedMembers` to select the members initially, set nothing to select all\n"
        + "## Note that it should be type List<PsiMember> or List<MemberEntry>\n"
        + "## And the selected result will be\n"
        + "## - fields1:  List<FieldEntry> where `1` is the step number that you specified\n"
        + "## - methods1: List<MethodEntry>\n"
        + "## - members:  List<MemberEntry>\n"
        + "#set(\$availableMembers = \$class0.members)\n")

const val PACKAGE = "package"

const val PARENT_METHOD = "parentMethod"

const val PERIOD = "."

const val SELECTED_MEMBERS = "selectedMembers"

const val SELECTION_FIELDS_FOR_CODE_GENERATION = "Selection Fields for Code Generation"

const val UNTITLED = "Untitled"

const val VELOCITY_TEMPLATE_LOCATION = "/template/default.vm"

const val VM = "vm"

const val XML = "xml"
