/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef UTILS_H
#define UTILS_H

#include <string>
#include <algorithm>
#include <jni.h>
#include <stdexcept>
#include <memory>

#include <AdblockPlus.h>

#define PKG(x) "org/adblockplus/libadblockplus/" x
#define TYP(x) "L" PKG(x) ";"

#define ABP_JNI_VERSION JNI_VERSION_1_6

void JniThrowException(JNIEnv* env, const std::string& message);

void JniThrowException(JNIEnv* env, const std::exception& e);

void JniThrowException(JNIEnv* env);

namespace AdblockPlus{
  class Subscription;
  typedef std::shared_ptr<Subscription> SubscriptionPtr;
  class Notification;
  typedef std::shared_ptr<Notification> NotificationPtr;
};

class JNIEnvAcquire;

/**
 * This class is _NOT_ thread safe!
 */
template<typename T>
class JniLocalReference
{
public:
  JniLocalReference(JNIEnv* jniEnv, T object)
      : jniEnv(jniEnv), object(object)
  {

  }

  JniLocalReference(const JniLocalReference<T>& other);

  ~JniLocalReference()
  {
    jniEnv->DeleteLocalRef(object);
  }

  T operator*()
  {
    return object;
  }

  T Get()
  {
    return object;
  }

private:
  JNIEnv* jniEnv;
  T object;
};

class JNIEnvAcquire
{
public:
  JNIEnvAcquire(JavaVM* javaVM);
  ~JNIEnvAcquire();

  JNIEnv* operator*()
  {
    return jniEnv;
  }

  JNIEnv* operator->()
  {
    return jniEnv;
  }

private:
  JavaVM* javaVM;
  JNIEnv* jniEnv;
  int attachmentStatus;
};

template<typename T>
class JniGlobalReference
{
public:
  JniGlobalReference(JNIEnv* env, T reference)
  {
    env->GetJavaVM(&javaVM);
    this->reference = static_cast<T>(env->NewGlobalRef(static_cast<jobject>(reference)));
  }

  ~JniGlobalReference()
  {
    JNIEnvAcquire env(javaVM);
    env->DeleteGlobalRef(static_cast<jobject>(reference));
  }

  JniGlobalReference(const JniGlobalReference& other);
  JniGlobalReference& operator=(const JniGlobalReference& other);

  T Get()
  {
    return reference;
  }

  typedef std::shared_ptr<JniGlobalReference<T> > Ptr;

private:
  T reference;
  JavaVM* javaVM;
};

inline void* JniLongToPtr(jlong value)
{
  return reinterpret_cast<void*>((size_t)value);
}

inline jlong JniPtrToLong(void* ptr)
{
  return (jlong)reinterpret_cast<size_t>(ptr);
}

// TODO: It's feeling a bit dirty casting to shared_ptr<T: JsValue> directly, so maybe
// implement a special cast functions that first reinterpret_casts to shared_ptr<JsValue> and then
// dynamic_casts to shared_ptr<T: JsValue> ... also as the same inheritance is mirrored on the Java
// side (and Java will throw a class cast exception on error) this shouldn't be an issue (TM)
template<typename T>
inline T* JniLongToTypePtr(jlong value)
{
  return reinterpret_cast<T*>((size_t)value);
}

std::string JniJavaToStdString(JNIEnv* env, jstring str);

void JniAddObjectToList(JNIEnv* env, jobject list, jobject value);

inline std::string JniGetStringField(JNIEnv* env, jclass clazz, jobject jObj, const char* name)
{
  return JniJavaToStdString(env, reinterpret_cast<jstring>(env->GetObjectField(jObj, env->GetFieldID(clazz, name, "Ljava/lang/String;"))));
}

inline bool JniGetBooleanField(JNIEnv* env, jclass clazz, jobject jObj, const char* name)
{
  return env->GetBooleanField(jObj, env->GetFieldID(clazz, name, "Z")) == JNI_TRUE;
}

inline int32_t JniGetIntField(JNIEnv* env, jclass clazz, jobject jObj, const char* name)
{
  return (int32_t)env->GetIntField(jObj, env->GetFieldID(clazz, name, "I"));
}

inline int64_t JniGetLongField(JNIEnv* env, jclass clazz, jobject jObj, const char* name)
{
  return (int64_t)env->GetLongField(jObj, env->GetFieldID(clazz, name, "J"));
}

jobject NewJniArrayList(JNIEnv* env);

jobject NewJniFilter(JNIEnv* env, const AdblockPlus::FilterPtr& filter);

jobject NewJniSubscription(JNIEnv* env,
    const AdblockPlus::SubscriptionPtr& subscription);

jobject NewJniNotification(JNIEnv* env,
    const AdblockPlus::NotificationPtr& notification);

#define CATCH_AND_THROW(jEnv) \
  catch (const std::exception& except) \
  { \
    JniThrowException(jEnv, except); \
  } \
  catch (...) \
  { \
    JniThrowException(jEnv); \
  }

#define CATCH_THROW_AND_RETURN(jEnv, retVal) \
  catch (const std::exception& except) \
  { \
    JniThrowException(jEnv, except); \
    return retVal; \
  } \
  catch (...) \
  { \
    JniThrowException(jEnv); \
    return retVal; \
  }

#endif
