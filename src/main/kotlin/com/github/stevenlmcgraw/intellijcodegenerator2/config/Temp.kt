package com.github.stevenlmcgraw.intellijcodegenerator2.config

const val GETTER_AND_SETTER_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<codeTemplateList>\n" +
        "    <templates>\n" +
        "        <templates version=\"1.3\">\n" +
        "            <id>58a3b5d1-3621-43b8-9c17-e725ceaedbdd</id>\n" +
        "            <name>Getter and Setter</name>\n" +
        "            <fileNamePattern>.*\\.java\\$</fileNamePattern>\n" +
        "            <type>body</type>\n" +
        "            <enabled>false</enabled>\n" +
        "            <template>#foreach(\$field in \$fields)\n" +
        "                #set(\$name = \$StringUtil.capitalizeWithJavaBeanConvention(\$StringUtil.sanitizeJavaIdentifier(\$helper.getPropertyName(\$field.element, \$project))))\n" +
        "                #if (\$field.boolean &amp;&amp; \$field.primitive)\n" +
        "                #set(\$getter = \"is\${name}\")\n" +
        "                #else\n" +
        "                #set(\$getter = \"get\${name}\")\n" +
        "                #end\n" +
        "                #set(\$setter = \"set\${name}\")\n" +
        "                #if(\$field.modifierStatic)\n" +
        "                static ##\n" +
        "                #end\n" +
        "                \$field.type ##\n" +
        "                \${getter}() {\n" +
        "                return \$field.name;\n" +
        "                }\n" +
        "\n" +
        "                #if(\$field.modifierStatic)\n" +
        "                static ##\n" +
        "                #end\n" +
        "                #set(\$paramName = \$helper.getParamName(\$field.element, \$project))\n" +
        "                void \${setter}(\$field.type \$paramName) {\n" +
        "                #if (\$field.name == \$paramName)\n" +
        "                #if (!\$field.modifierStatic)\n" +
        "                this.##\n" +
        "                #else\n" +
        "                \$classname.##\n" +
        "                #end\n" +
        "                #end\n" +
        "                \$field.name = \$paramName;\n" +
        "                }\n" +
        "                #end</template>\n" +
        "            <fileEncoding>UTF-8</fileEncoding>\n" +
        "            <pipeline>\n" +
        "                <memberSelection>\n" +
        "                    <filterConstantField>true</filterConstantField>\n" +
        "                    <filterEnumField>false</filterEnumField>\n" +
        "                    <filterTransientModifier>false</filterTransientModifier>\n" +
        "                    <filterStaticModifier>true</filterStaticModifier>\n" +
        "                    <filterLoggers>true</filterLoggers>\n" +
        "                    <filterFieldName></filterFieldName>\n" +
        "                    <filterFieldType></filterFieldType>\n" +
        "                    <filterMethodName></filterMethodName>\n" +
        "                    <filterMethodType></filterMethodType>\n" +
        "                    <enableMethods>false</enableMethods>\n" +
        "                    <providerTemplate>#set(\$availableMembers = [])\n" +
        "                        #set(\$methodNames = [])\n" +
        "                        #foreach(\$method in \$class0.methods)\n" +
        "                        \$methodNames.add(\$method.methodName)\n" +
        "                        #end\n" +
        "                        #foreach(\$field in \$class0.fields)\n" +
        "                        #set(\$name = \$StringUtil.capitalizeWithJavaBeanConvention(\$StringUtil.sanitizeJavaIdentifier(\$helper.getPropertyName(\$field.element, \$project))))\n" +
        "                        #if (\$field.boolean &amp;&amp; \$field.primitive)\n" +
        "                        #set(\$getter = \"is\${name}\")\n" +
        "                        #else\n" +
        "                        #set(\$getter = \"get\${name}\")\n" +
        "                        #end\n" +
        "                        #set(\$setter = \"set\${name}\")\n" +
        "                        #if (!\$methodNames.contains(\$getter) || !\$methodNames.contains(\$setter))\n" +
        "                        \$availableMembers.add(\$field)\n" +
        "                        #end\n" +
        "                        #end\n" +
        "                    </providerTemplate>\n" +
        "                    <allowMultiSelection>true</allowMultiSelection>\n" +
        "                    <allowEmptySelection>false</allowEmptySelection>\n" +
        "                    <sortElements>0</sortElements>\n" +
        "                    <postfix>1</postfix>\n" +
        "                    <enabled>true</enabled>\n" +
        "                </memberSelection>\n" +
        "            </pipeline>\n" +
        "            <insertNewMethodOption>AT_CARET</insertNewMethodOption>\n" +
        "            <whenDuplicatesOption>ASK</whenDuplicatesOption>\n" +
        "            <jumpToMethod>true</jumpToMethod>\n" +
        "            <classNameVm>\$class0.name</classNameVm>\n" +
        "            <alwaysPromptForPackage>false</alwaysPromptForPackage>\n" +
        "        </templates>\n" +
        "    </templates>\n" +
        "</codeTemplateList>\n"