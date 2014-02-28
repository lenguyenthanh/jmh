/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.generators.bytecode;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmh.generators.source.ClassInfo;
import org.openjdk.jmh.generators.source.FieldInfo;
import org.openjdk.jmh.generators.source.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASMClassInfo extends ClassVisitor implements ClassInfo {

    private String idName;
    private String packageName;
    private String qualifiedName;
    private String name;
    private int access;

    private final List<MethodInfo> methods;
    private final List<MethodInfo> constructors;
    private final List<FieldInfo> fields;
    private final Map<String, AnnHandler> annotations = new HashMap<String, AnnHandler>();
    private final ClassInfoRepo classInfos;

    public ASMClassInfo(ClassInfoRepo classInfos) {
        super(Opcodes.ASM4);
        this.classInfos = classInfos;
        methods = new ArrayList<MethodInfo>();
        constructors = new ArrayList<MethodInfo>();
        fields = new ArrayList<FieldInfo>();
    }

    public String getIdName() {
        return idName;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.idName = name;
        this.access = access;
        qualifiedName = name.replaceAll("/", ".");
        packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        this.name = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        AnnHandler handler = annotations.get(annClass.getCanonicalName());
        if (handler == null) {
            return null;
        } else {
            return (T) Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[]{annClass},
                    handler);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
        AnnHandler annHandler = new AnnHandler(super.visitAnnotation(desc, visible));
        annotations.put(Type.getType(desc).getClassName(), annHandler);
        return annHandler;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        ASMFieldInfo fi = new ASMFieldInfo(super.visitField(access, name, desc, signature, value),
                this, access, name, desc, signature);
        fields.add(fi);
        return fi;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature, String[] exceptions) {
        ASMMethodInfo mi = new ASMMethodInfo(super.visitMethod(access, methodName, methodDesc, signature, exceptions),
                classInfos, this, access, methodName, methodDesc, signature);
        methods.add(mi);
        if (methodName.equals("<init>")) {
            constructors.add(mi);
        }
        return mi;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getNestedName() {
        return name;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public Collection<FieldInfo> getDeclaredFields() {
        return fields;
    }

    @Override
    public Collection<MethodInfo> getConstructors() {
        return constructors;
    }

    @Override
    public Collection<MethodInfo> getDeclaredMethods() {
        return methods;
    }

    @Override
    public Collection<ClassInfo> getSuperclasses() {
        // TODO: FIXME
        return Collections.emptyList();
    }


    @Override
    public <T extends Annotation> T getAnnotationRecursive(Class<T> annClass) {
        return getAnnotation(annClass); // TODO: FIXME
    }

    @Override
    public boolean isAbstract() {
        return (access & Opcodes.ACC_ABSTRACT) > 0;
    }

    @Override
    public boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) > 0;
    }

    @Override
    public boolean isStrictFP() {
        return (access & Opcodes.ACC_STRICT) > 0;
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}